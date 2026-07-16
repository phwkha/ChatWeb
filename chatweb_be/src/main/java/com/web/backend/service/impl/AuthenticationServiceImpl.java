package com.web.backend.service.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.web.backend.common.OtpType;
import com.web.backend.common.TokenType;
import com.web.backend.common.UserStatus;
import com.web.backend.config.LocalResolverConfig.Translator;
import com.web.backend.controller.request.CreateUserRequest;
import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.request.VerifyOtpRequest;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.controller.response.TokenResponse;
import com.web.backend.controller.response.UserResponse;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.AuthenticationFailedException;
import com.web.backend.exception.custom.InvalidDataException;
import com.web.backend.exception.custom.InvalidOtpException;
import com.web.backend.exception.custom.ResourceConflictException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.kafka.producer.EmailProducer;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.RegisterData;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.AuthenticationService;
import com.web.backend.service.JwtService;
import com.web.backend.service.util.CuckooFilterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j(topic = "AUTHENTICATION-SERVICE")
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final UserMapper userMapper;

    private final JwtService jwtService;

    private final CacheManager cacheManager;

    private final EmailProducer emailKafkaProducer;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.expiration-minutes}")
    private int expirationMinutes;

    private final CuckooFilterService cuckooFilterService;

    private static final String EMAIL_FILTER_KEY = "filter:emails";
    private static final String USERNAME_FILTER_KEY = "filter:usernames";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(CreateUserRequest createUserRequest) {

        boolean mightExistEmail = cuckooFilterService.exists(EMAIL_FILTER_KEY, createUserRequest.getEmail());
        if (mightExistEmail) {
            if (userRepository.existsByEmail(createUserRequest.getEmail())) {
                throw new ResourceConflictException(Translator.tolocale("error.auth.email_used"));
            }
        }

        boolean mightExistUsername = cuckooFilterService.exists(USERNAME_FILTER_KEY, createUserRequest.getUsername());
        if (mightExistUsername) {
            if (userRepository.existsByUsername(createUserRequest.getUsername())) {
                throw new ResourceConflictException(Translator.tolocale("error.auth.username_exists"));
            }
        }

        SecureRandom secureRandom = new SecureRandom();
        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));
        RoleEntity defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.auth.role_user_missing")));

        RegisterData data = RegisterData.builder()
                .username(createUserRequest.getUsername())
                .email(createUserRequest.getEmail())
                .password(passwordEncoder.encode(createUserRequest.getPassword()))
                .roleId(defaultRole.getId())
                .otp(otp)
                .build();

        String redisKey = "register:" + createUserRequest.getEmail();
        redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(data), 5, TimeUnit.MINUTES);

        emailKafkaProducer.sendOtpEmailTask(createUserRequest.getEmail(), createUserRequest.getUsername(), otp);

        log.info("Registering new user: {}", createUserRequest.getUsername());
        return UserResponse.builder()
                .username(createUserRequest.getUsername())
                .email(createUserRequest.getEmail())
                .userStatus(UserStatus.UNVERIFIED)
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        List<String> authorities = new ArrayList<>();

        Integer tokenVersion;
        UserEntity userPrincipal;
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));
            authorities.add(authentication.getAuthorities().toString());
            userPrincipal = (UserEntity) authentication.getPrincipal();
            tokenVersion = userPrincipal.getTokenVersion();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (LockedException e) {
            throw new AccessForbiddenException(Translator.tolocale("error.auth.account_locked"));
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException(Translator.tolocale("error.auth.invalid_credentials"));
        }

        String accessToken = jwtService.generateAccessToken(
                loginRequest.getUsername(),
                authorities,
                tokenVersion

        );

        String refreshToken = jwtService.generateRefreshToken(
                loginRequest.getUsername(),
                authorities,
                tokenVersion);

        UserResponse userResponse = userMapper.toUserResponse(userPrincipal);
        log.info("Login with user: {}", loginRequest.getUsername());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userResponse(userResponse)
                .build();
    }

    @Override
    public void logout(String token, TokenType tokenType) {
        long remainingTime = jwtService.getRemainingTime(token, tokenType);

        if (remainingTime > 0) {
            String key = "blacklist:" + token;
            redisTemplate.opsForValue().set(key, "logged_out", remainingTime, TimeUnit.MILLISECONDS);
        }
        log.info("Token added to blacklist with TTL: {} ms", remainingTime);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new InvalidDataException(Translator.tolocale("error.auth.missing_refresh"));
        }
        String username = jwtService.extractUsername(refreshToken, TokenType.REFRESH_TOKEN);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        String blacklistKey = "blacklist:" + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            user.setTokenVersion(user.getTokenVersion() == null ? 0 : user.getTokenVersion() + 1);
            userRepository.save(user);
            Cache userCache = cacheManager.getCache("user_details");
            if (userCache != null && user.getUsername() != null) {
                userCache.evict(Objects.requireNonNull(user.getUsername()));
            }
            throw new AccessForbiddenException(Translator.tolocale("error.auth.refresh_revoked"));
        }

        Integer tokenVersionInJwt = jwtService.extractClaim(refreshToken, TokenType.REFRESH_TOKEN,
                claims -> claims.get("v", Integer.class));
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();

        if (tokenVersionInJwt == null || !tokenVersionInJwt.equals(currentVersion)) {
            throw new AccessForbiddenException(Translator.tolocale("error.auth.refresh_expired"));
        }

        List<String> authorities = new ArrayList<>();
        user.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));
        if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException(Translator.tolocale("error.auth.locked_or_not_found"));
        }

        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), authorities, currentVersion);
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername(), authorities, currentVersion);

        long remainingTime = jwtService.getRemainingTime(refreshToken, TokenType.REFRESH_TOKEN);
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(blacklistKey, "rotated", remainingTime, TimeUnit.MILLISECONDS);
        }

        log.info("Refresh token with user: {}", username);
        return TokenResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
    }

    @Override
    @Transactional
    public void initiateForgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.auth.email_not_found")));

        if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException(Translator.tolocale("error.auth.locked_not_found"));
        }

        generateAndSenResponseToken(user, OtpType.PASSWORD_RESET, null, user.getEmail());

        log.info("Password reset initiated for email: {}", email);
    }

    private void generateAndSenResponseToken(UserEntity user, OtpType type, String extraData, String targetEmail) {

        SecureRandom secureRandom = new SecureRandom();
        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

        String redisKey = "otp:" + type.name() + ":" + user.getUsername();

        String redisValue = otp + (extraData != null ? ":" + extraData : "");

        redisTemplate.opsForValue().set(redisKey, redisValue, expirationMinutes, TimeUnit.MINUTES);

        emailKafkaProducer.sendOtpEmailTask(targetEmail, user.getUsername(), otp);
    }

    @Override
    @Transactional
    public void verifyUser(VerifyOtpRequest request) {
        String redisKey = "register:" + request.getEmail();

        RegisterData data = (RegisterData) redisTemplate.opsForValue().get(redisKey);

        if (data == null) {
            throw new ResourceNotFoundException(Translator.tolocale("error.auth.otp_expired_or_email_missing"));
        }

        if (!data.getOtp().equals(request.getOtp())) {
            throw new InvalidOtpException(Translator.tolocale("error.auth.invalid_otp"));
        }

        if (userRepository.existsByUsername(data.getUsername()) || userRepository.existsByEmail(data.getEmail())) {
            throw new ResourceConflictException(Translator.tolocale("error.auth.registered_by_other"));
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(data.getUsername());
        newUser.setEmail(data.getEmail());
        newUser.setPassword(data.getPassword());
        newUser.setUserStatus(UserStatus.ACTIVE);
        newUser.setCreateAt(new Date());

        Long roleId = data.getRoleId();
        RoleEntity role;
        if (roleId != null) {
            role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.role.not_found")));
        } else {
            role = roleRepository.findByName("USER")
                    .orElseThrow(
                            () -> new ResourceNotFoundException(Translator.tolocale("error.role.default_not_found")));
        }
        newUser.setRole(role);

        userRepository.save(Objects.requireNonNull(newUser));

        cuckooFilterService.add(USERNAME_FILTER_KEY, newUser.getUsername());
        cuckooFilterService.add(EMAIL_FILTER_KEY, newUser.getEmail());

        redisTemplate.delete(redisKey);

        log.info("User {} activated with role {}", newUser.getUsername(), role.getName());
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        String redisKey = "register:" + email;

        String cooldownKey = "cooldown:resend:" + email;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ResourceConflictException(Translator.tolocale("error.auth.wait_60s"));
        }

        RegisterData data = (RegisterData) redisTemplate.opsForValue().get(redisKey);

        if (data != null) {
            SecureRandom secureRandom = new SecureRandom();
            String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

            data.setOtp(otp);
            redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(data), expirationMinutes,
                    TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
            emailKafkaProducer.sendOtpEmailTask(data.getEmail(), data.getUsername(), otp);
            log.info("Resend Register OTP to Redis user: {}", data.getUsername());
            return;
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getUserStatus() == UserStatus.ACTIVE) {
                throw new ResourceConflictException(Translator.tolocale("error.auth.already_active"));
            }
            if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
                throw new ResourceConflictException(Translator.tolocale("error.auth.account_locked_deleted"));
            }
        }

        throw new ResourceNotFoundException(Translator.tolocale("error.auth.reg_expired"));
    }

    @Override
    @Transactional
    public void verifyPasswordReset(String email, String otp, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException(Translator.tolocale("error.auth.locked_not_found"));
        }

        validateRedisOtp(user.getUsername(), OtpType.PASSWORD_RESET, otp);

        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        Cache userCache = cacheManager.getCache("user_details");
        if (userCache != null && user.getUsername() != null) {
            userCache.evict(Objects.requireNonNull(user.getUsername()));
        }
        log.info("Password reset successfully via Redis OTP for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void resendForgotPasswordOtp(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException(Translator.tolocale("error.user.email_user_not_found")));

        if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException(Translator.tolocale("error.auth.locked_not_found"));
        }

        resendRedisOtp(user.getUsername(), OtpType.PASSWORD_RESET, email);
    }

    private String validateRedisOtp(String identifier, OtpType type, String inputOtp) {
        String redisKey = "otp:" + type.name() + ":" + identifier;

        String attemptKey = redisKey + ":attempts";

        String value = (String) redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            throw new InvalidOtpException(Translator.tolocale("error.auth.otp_expired_or_req_missing"));
        }

        String[] parts = value.split(":");
        String savedOtp = parts[0];
        String extraData = parts.length > 1 ? parts[1] : null;

        if (!savedOtp.equals(inputOtp)) {
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            redisTemplate.expire(attemptKey, 5, TimeUnit.MINUTES);

            if (attempts != null && attempts >= 5) {
                redisTemplate.delete(redisKey);
                redisTemplate.delete(attemptKey);
                throw new InvalidOtpException(Translator.tolocale("error.auth.otp_canceled_5_times"));
            }

            throw new InvalidOtpException(Translator.tolocale("error.auth.invalid_otp_attempts", attempts));
        }

        redisTemplate.delete(redisKey);
        redisTemplate.delete(attemptKey);
        return extraData;
    }

    private void resendRedisOtp(String identifier, OtpType type, String emailToSend) {
        String redisKey = "otp:" + type.name() + ":" + identifier;

        String cooldownKey = "cooldown:resend:" + identifier;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ResourceConflictException(Translator.tolocale("error.auth.wait_60s"));
        }

        String oldValue = (String) redisTemplate.opsForValue().get(redisKey);

        if (oldValue == null) {
            throw new ResourceNotFoundException(Translator.tolocale("error.auth.req_expired"));
        }

        String[] parts = oldValue.split(":");
        String extraData = parts.length > 1 ? parts[1] : "";

        SecureRandom secureRandom = new SecureRandom();
        String newOtp = String.valueOf(100000 + secureRandom.nextInt(900000));

        String newValue = newOtp + (extraData.isEmpty() ? "" : ":" + extraData);

        redisTemplate.opsForValue().set(redisKey, newValue, expirationMinutes, TimeUnit.MINUTES);

        String usernameForMail = identifier;

        if (type == OtpType.PASSWORD_RESET) {
            Optional<UserEntity> u = userRepository.findByEmail(identifier);
            if (u.isPresent())
                usernameForMail = u.get().getUsername();
        }
        redisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
        emailKafkaProducer.sendOtpEmailTask(emailToSend, usernameForMail, newOtp);
        log.info("Resent {} OTP to {}", type, emailToSend);
    }

}
