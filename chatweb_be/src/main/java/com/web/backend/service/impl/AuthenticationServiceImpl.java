package com.web.backend.service.impl;

import com.web.backend.common.OtpType;
import com.web.backend.common.TokenType;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.CreateUserRequest;
import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.request.VerifyOtpRequest;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.controller.response.UserResponse;
import com.web.backend.exception.*;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.RegisterData;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.AuthenticationService;
import com.web.backend.service.JwtService;
import com.web.backend.service.util.CuckooFilterService;
import com.web.backend.service.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j(topic = "AUTHENTICATION-SERVICE")
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.sendgrid.expiration-minutes}")
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
                throw new ResourceConflictException("Email đã được sử dụng");
            }
        }

        boolean mightExistUsername = cuckooFilterService.exists(USERNAME_FILTER_KEY, createUserRequest.getUsername());
        if (mightExistUsername) {
            if (userRepository.existsByUsername(createUserRequest.getUsername())) {
                throw new ResourceConflictException("Tên đăng nhập đã tồn tại");
            }
        }

        SecureRandom secureRandom = new SecureRandom();
        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));
        RoleEntity defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role USER chưa có"));

        RegisterData data = RegisterData.builder()
                .username(createUserRequest.getUsername())
                .email(createUserRequest.getEmail())
                .password(passwordEncoder.encode(createUserRequest.getPassword()))
                .roleId(defaultRole.getId())
                .otp(otp)
                .build();

        String redisKey = "register:" + createUserRequest.getEmail();
        redisTemplate.opsForValue().set(redisKey, data, 5, TimeUnit.MINUTES);

        emailService.sendOtpEmail(createUserRequest.getEmail(), createUserRequest.getUsername(), otp);

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

    @Override
    @Transactional
    public void initiateForgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại trong hệ thống"));

        generateAndSenResponseToken(user, OtpType.PASSWORD_RESET, null, user.getEmail());

        log.info("Password reset initiated for email: {}", email);
    }

    private void generateAndSenResponseToken(UserEntity user, OtpType type, String extraData, String targetEmail) {

        SecureRandom secureRandom = new SecureRandom();
        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

        String redisKey = "otp:" + type.name() + ":" + user.getUsername();

        String redisValue = otp + (extraData != null ? ":" + extraData : "");

        redisTemplate.opsForValue().set(redisKey, redisValue, expirationMinutes, TimeUnit.MINUTES);

        emailService.sendOtpEmail(targetEmail, user.getUsername(), otp);
    }

    @Override
    @Transactional
    public void verifyUser(VerifyOtpRequest request) {
        String redisKey = "register:" + request.getEmail();

        RegisterData data = (RegisterData) redisTemplate.opsForValue().get(redisKey);

        if (data == null) {
            throw new ResourceNotFoundException("Mã OTP đã hết hạn hoặc email không tồn tại");
        }

        if (!data.getOtp().equals(request.getOtp())) {
            throw new InvalidOtpException("Mã OTP không chính xác");
        }

        if (userRepository.existsByUsername(data.getUsername()) || userRepository.existsByEmail(data.getEmail())) {
            throw new ResourceConflictException("Tài khoản đã bị đăng ký bởi người khác.");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(data.getUsername());
        newUser.setEmail(data.getEmail());
        newUser.setPassword(data.getPassword());
        newUser.setUserStatus(UserStatus.ACTIVE);
        newUser.setCreateAt(new Date());

        RoleEntity role = roleRepository.findById(data.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));
        newUser.setRole(role);

        userRepository.save(newUser);

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
            throw new ResourceConflictException("Vui lòng đợi 60 giây trước khi gửi lại OTP.");
        }

        RegisterData data = (RegisterData) redisTemplate.opsForValue().get(redisKey);

        if (data != null) {
            SecureRandom secureRandom = new SecureRandom();
            String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

            data.setOtp(otp);
            redisTemplate.opsForValue().set(redisKey, data, expirationMinutes, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
            emailService.sendOtpEmail(data.getEmail(), data.getUsername(), otp);
            log.info("Resend Register OTP to Redis user: {}", data.getUsername());
            return;
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getUserStatus() == UserStatus.ACTIVE) {
                throw new ResourceConflictException("Tài khoản này đã được kích hoạt, vui lòng đăng nhập.");
            }
            if (user.getUserStatus() == UserStatus.INACTIVE || user.getUserStatus() == UserStatus.LOCKED) {
                throw new ResourceConflictException("Tài khoản này đã bị khóa hoặc xóa. Vui lòng liên hệ Admin.");
            }
        }

        throw new ResourceNotFoundException("Yêu cầu đăng ký không tồn tại hoặc đã hết hạn. Vui lòng đăng ký lại.");
    }


    @Override
    @Transactional
    public void verifyPasswordReset(String email, String otp, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        validateRedisOtp(user.getUsername(), OtpType.PASSWORD_RESET, otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully via Redis OTP for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void resendForgotPasswordOtp(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng email"));
        resendRedisOtp(user.getUsername(), OtpType.PASSWORD_RESET, email);
    }

    private String validateRedisOtp(String identifier, OtpType type, String inputOtp) {
        String redisKey = "otp:" + type.name() + ":" + identifier;

        String attemptKey = redisKey + ":attempts";

        String value = (String) redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            throw new InvalidOtpException("Mã OTP đã hết hạn hoặc yêu cầu không tồn tại");
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
                throw new InvalidOtpException("Bạn đã nhập sai quá 5 lần. Mã OTP đã bị hủy.");
            }

            throw new InvalidOtpException("Mã OTP không chính xác (Lần thử " + attempts + "/5)");
        }

        redisTemplate.delete(redisKey);
        redisTemplate.delete(attemptKey);
        return extraData;
    }

    private void resendRedisOtp(String identifier, OtpType type, String emailToSend) {
        String redisKey = "otp:" + type.name() + ":" + identifier;

        String cooldownKey = "cooldown:resend:" + identifier;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ResourceConflictException("Vui lòng đợi 60 giây trước khi gửi lại OTP.");
        }

        String oldValue = (String) redisTemplate.opsForValue().get(redisKey);

        if (oldValue == null) {
            throw new ResourceNotFoundException("Yêu cầu không tồn tại hoặc đã hết hạn. Vui lòng thực hiện lại.");
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
            if (u.isPresent()) usernameForMail = u.get().getUsername();
        }
        redisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
        emailService.sendOtpEmail(emailToSend, usernameForMail, newOtp);
        log.info("Resent {} OTP to {}", type, emailToSend);
    }

}
