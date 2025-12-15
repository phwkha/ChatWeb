package com.web.backend.service.impl;

import com.web.backend.common.OtpType;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.VerifyOtpRequest;
import com.web.backend.model.RegisterData;
import com.web.backend.exception.InvalidDataException;
import com.web.backend.exception.InvalidOtpException;
import com.web.backend.exception.ResourceConflictException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.CuckooFilterService;
import com.web.backend.service.EmailService;
import com.web.backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "OTP-SERVICE")
public class OtpServiceImpl implements OtpService {

    private final EmailService emailService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.sendgrid.expiration-minutes}")
    private int expirationMinutes;

    private final CuckooFilterService cuckooFilterService;

    private static final String EMAIL_FILTER_KEY = "filter:emails";
    private static final String USERNAME_FILTER_KEY = "filter:usernames";

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
        RegisterData data = (RegisterData) redisTemplate.opsForValue().get(redisKey);

        if (data != null) {
            SecureRandom secureRandom = new SecureRandom();
            String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

            data.setOtp(otp);
            redisTemplate.opsForValue().set(redisKey, data, expirationMinutes, TimeUnit.MINUTES);

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

    private String validateRedisOtp(String identifier, OtpType type, String inputOtp) {
        String redisKey = "otp:" + type.name() + ":" + identifier;
        String value = (String) redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            throw new InvalidOtpException("Mã OTP đã hết hạn hoặc yêu cầu không tồn tại");
        }

        String[] parts = value.split(":");
        String savedOtp = parts[0];
        String extraData = parts.length > 1 ? parts[1] : null;

        if (!savedOtp.equals(inputOtp)) {
            throw new InvalidOtpException("Mã OTP không chính xác");
        }

        redisTemplate.delete(redisKey);
        return extraData;
    }

    @Override
    @Transactional
    public void verifyEmailChange(String username, String otp) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        String oldEmail = user.getEmail();
        String newEmail = validateRedisOtp(username, OtpType.EMAIL_CHANGE, otp);

        if (newEmail == null || newEmail.isEmpty()) {
            throw new InvalidDataException("Dữ liệu email mới bị lỗi");
        }

        user.setEmail(newEmail);
        userRepository.save(user);

        cuckooFilterService.delete(EMAIL_FILTER_KEY, oldEmail);
        cuckooFilterService.add(EMAIL_FILTER_KEY, newEmail);
        log.info("Email changed successfully via Redis OTP for user: {}", username);
    }

    @Override
    @Transactional
    public void verifyPasswordReset(String email, String otp, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        validateRedisOtp(email, OtpType.PASSWORD_RESET, otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully via Redis OTP for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void verifyPhoneChange(String username, String otp) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        String newPhone = validateRedisOtp(username, OtpType.PHONE_CHANGE, otp);

        if (newPhone == null || newPhone.isEmpty()) {
            throw new InvalidDataException("Dữ liệu số điện thoại mới bị lỗi");
        }

        user.setPhone(newPhone);
        userRepository.save(user);
        log.info("Phone changed successfully via Redis OTP for user: {}", username);
    }

    private void resendRedisOtp(String identifier, OtpType type, String emailToSend) {
        String redisKey = "otp:" + type.name() + ":" + identifier;
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

        emailService.sendOtpEmail(emailToSend, usernameForMail, newOtp);
        log.info("Resent {} OTP to {}", type, emailToSend);
    }

    @Override
    @Transactional
    public void resendEmailChangeOtp(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        String redisKey = "otp:" + OtpType.EMAIL_CHANGE.name() + ":" + username;
        String oldValue = (String) redisTemplate.opsForValue().get(redisKey);
        if (oldValue == null) throw new ResourceNotFoundException("Yêu cầu không tồn tại");

        String[] parts = oldValue.split(":");
        String newEmail = parts.length > 1 ? parts[1] : null;

        if (newEmail == null) throw new InvalidDataException("Không tìm thấy email mới trong yêu cầu");

        resendRedisOtp(username, OtpType.EMAIL_CHANGE, newEmail);
    }

    @Override
    @Transactional
    public void resendForgotPasswordOtp(String email) {
        resendRedisOtp(email, OtpType.PASSWORD_RESET, email);
    }

    @Override
    @Transactional
    public void resendPhoneChangeOtp(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        resendRedisOtp(username, OtpType.PHONE_CHANGE, user.getEmail());
    }
}