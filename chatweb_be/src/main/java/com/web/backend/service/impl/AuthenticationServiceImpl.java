package com.web.backend.service.impl;

import com.web.backend.common.TokenType;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.exception.AccessForbiddenException;
import com.web.backend.exception.AuthenticationFailedException;
import com.web.backend.exception.InvalidDataException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.AuthenticationService;
import com.web.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j(topic = "AUTHENTICATION-SERVICE")
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final RedisTemplate<String, Object> redisTemplate;

    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        List<String> authorities = new ArrayList<>();

        Integer tokenVersion;

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            authorities.add(authentication.getAuthorities().toString());
            UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
            tokenVersion = userPrincipal.getTokenVersion();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (org.springframework.security.authentication.LockedException e) {
            throw new AccessForbiddenException("Tài khoản của bạn đã bị khóa!");
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationFailedException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        String accessToken = jwtService.generateAccessToken(
                loginRequest.getUsername(),
                authorities,
                tokenVersion

        );

        String refreshToken = jwtService.generateRefreshToken(
                loginRequest.getUsername(),
                authorities,
                tokenVersion
        );
        log.info("Login with user: {}", loginRequest.getUsername());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout(String accessToken) {
        long remainingTime = jwtService.getRemainingTime(accessToken);

        if (remainingTime > 0) {
            // 2. Lưu vào Redis (Blacklist)
            String key = "blacklist:" + accessToken;
            redisTemplate.opsForValue().set(key, "logged_out", remainingTime, TimeUnit.MILLISECONDS);
        }

        log.info("Token added to blacklist with TTL: {} ms", remainingTime);
    }

    @Override
    public String refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new InvalidDataException("Refresh Token không tồn tại");
        }

        String username = jwtService.extractUsername(refreshToken, TokenType.REFRESH_TOKEN);

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Integer tokenVersionInJwt = jwtService.extractClaim(refreshToken, TokenType.REFRESH_TOKEN, claims -> claims.get("v", Integer.class));
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();

        if (tokenVersionInJwt == null || !tokenVersionInJwt.equals(currentVersion)) {
            throw new AccessForbiddenException("Refresh token đã hết hạn do thay đổi mật khẩu/đăng xuất");
        }

        List<String> authorities = new ArrayList<>();

        user.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));

        if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException("Tài khoản đã bị khóa hoặc User không tồn tại");
        }
        log.info("Refresh token with user: {}", username);
        return jwtService.generateAccessToken(
                user.getUsername(),
                authorities,
                currentVersion
        );
    }
}
