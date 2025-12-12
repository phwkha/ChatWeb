package com.web.backend.service.impl;

import com.web.backend.common.OtpType;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.VerifyOtpRequest;
import com.web.backend.exception.InvalidOtpException;
import com.web.backend.exception.ResourceConflictException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.model.PendingUserEntity;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.model.VerificationToken;
import com.web.backend.repository.PendingUserRepository;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.repository.VerificationTokenRepository;
import com.web.backend.service.EmailService;
import com.web.backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "OTP-SERVICE")
public class OtpServiceImpl implements OtpService {

    private final EmailService emailService;

    private final PendingUserRepository pendingUserRepository;

    private final UserRepository userRepository;

    private final VerificationTokenRepository tokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    @Value("${spring.sendgrid.expiration-minutes}")
    private int expirationMinutes;

    @Override
    @Transactional
    public void verifyUser(VerifyOtpRequest request) {
        PendingUserEntity pendingUser = pendingUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đăng ký không tồn tại hoặc đã hết hạn"));

        if (!pendingUser.getOtpCode().equals(request.getOtp())) {
            throw new InvalidOtpException("Mã OTP không chính xác");
        }
        if (pendingUser.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new InvalidOtpException("Mã OTP đã hết hạn");
        }

        if (userRepository.existsByUsername(pendingUser.getUsername()) || userRepository.existsByEmail(pendingUser.getEmail())) {
            throw new ResourceConflictException("Tài khoản hoặc Email đã bị đăng ký bởi người khác trong lúc chờ.");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(pendingUser.getUsername());
        newUser.setEmail(pendingUser.getEmail());
        newUser.setPassword(pendingUser.getPassword());
        newUser.setUserStatus(UserStatus.ACTIVE);
        newUser.setCreateAt(new Date());
        RoleEntity role = roleRepository.findById(pendingUser.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));
        newUser.setRole(role);
        userRepository.save(newUser);

        pendingUserRepository.delete(pendingUser);

        log.info("User {} activated with role {}", newUser.getUsername(), role.getName());
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        Optional<PendingUserEntity> pendingUserOpt = pendingUserRepository.findByEmail(email);

        if (pendingUserOpt.isPresent()) {
            PendingUserEntity pendingUser = pendingUserOpt.get();

            SecureRandom secureRandom = new SecureRandom();
            int otpValue = 100000 + secureRandom.nextInt(900000);
            String otp = String.valueOf(otpValue);
            pendingUser.setOtpCode(otp);
            pendingUser.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(expirationMinutes));

            pendingUserRepository.save(pendingUser);

            emailService.sendOtpEmail(pendingUser.getEmail(), pendingUser.getUsername(), otp);
            log.info("Resend OTP to pending user: {}", pendingUser.getUsername());
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
    public void verifyEmailChange(String username, String otp) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        VerificationToken token = tokenRepository.findByUserAndType(user, OtpType.EMAIL_CHANGE)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại hoặc đã hết hạn"));

        validateOtp(token, otp);

        String newEmail = token.getExtraData();
        user.setEmail(newEmail);
        userRepository.save(user);

        tokenRepository.delete(token);
        log.info("Email changed successfully for user");
    }

    @Override
    @Transactional
    public void verifyPasswordReset(String email, String otp, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken token = tokenRepository.findByUserAndType(user, OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại hoặc đã hết hạn"));

        validateOtp(token, otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(token);
        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void verifyPhoneChange(String username, String otp) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken token = tokenRepository.findByUserAndType(user, OtpType.PHONE_CHANGE)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại hoặc đã hết hạn"));

        validateOtp(token, otp);

        String newPhone = token.getExtraData();
        user.setPhone(newPhone);
        userRepository.save(user);

        tokenRepository.delete(token);
        log.info("Phone changed successfully for user");
    }

    private void validateOtp(VerificationToken token, String otp) {
        if (!token.getToken().equals(otp)) {
            throw new InvalidOtpException("Mã OTP không chính xác");
        }
        if (token.getExpiryDate().isBefore(new java.util.Date().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())) {
            throw new InvalidOtpException("Mã OTP đã hết hạn");
        }
    }

    private void updateTokenAndSendEmail(VerificationToken token, String targetEmail) {
        SecureRandom secureRandom = new SecureRandom();
        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

        token.setToken(otp);
        token.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(expirationMinutes));
        tokenRepository.save(token);

        emailService.sendOtpEmail(targetEmail, token.getUser().getUsername(), otp);
    }

    @Override
    @Transactional
    public void resendEmailChangeOtp(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        VerificationToken token = tokenRepository.findByUserAndType(user, OtpType.EMAIL_CHANGE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi email nào đang chờ xử lý."));

        String newEmail = token.getExtraData();

        updateTokenAndSendEmail(token, newEmail);

        log.info("Resent Email Change OTP to {} for user {}", newEmail, username);
    }

    @Override
    @Transactional
    public void resendForgotPasswordOtp(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại trong hệ thống"));

        VerificationToken token = tokenRepository.findByUserAndType(user, OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu quên mật khẩu nào. Vui lòng thực hiện lại từ đầu."));

        updateTokenAndSendEmail(token, user.getEmail());

        log.info("Resent Forgot Password OTP to email: {}", email);
    }

    @Override
    @Transactional
    public void resendPhoneChangeOtp(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        VerificationToken token = tokenRepository.findByUserAndType(user, OtpType.PHONE_CHANGE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi số điện thoại nào."));

        updateTokenAndSendEmail(token, user.getEmail());

        log.info("Resent Phone Change OTP for user");
    }
}
