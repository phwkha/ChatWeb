package com.web.backend.service.impl;

import com.web.backend.common.OtpType;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.*;
import com.web.backend.controller.response.*;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.InvalidDataException;
import com.web.backend.exception.custom.InvalidOtpException;
import com.web.backend.exception.custom.InvalidPasswordException;
import com.web.backend.exception.custom.PasswordMismatchException;
import com.web.backend.exception.custom.ResourceConflictException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.*;
import com.web.backend.repository.MessageRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.StorageService;
import com.web.backend.service.util.CuckooFilterService;
import com.web.backend.service.util.EmailService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.web.backend.config.LocalResolverConfig.Translator;

@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final MessageRepository messageRepository;

    private final EmailService emailService;

    private final StorageService storageService;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.expiration-minutes}")
    private int expirationMinutes;

    private final CuckooFilterService cuckooFilterService;

    private static final String EMAIL_FILTER_KEY = "filter:emails";

    @Override
    public UserResponse getMe(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new ResourceNotFoundException(Translator.tolocale("error.user.not_found_with", username));
        }

        log.info("Get current user");
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserDetailResponse getProfileUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new ResourceNotFoundException(Translator.tolocale("error.user.not_found_with", username));
        }

        log.info("Get profile user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#username")
    public UserDetailResponse updateUser(String username, UpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        userMapper.updateUserFromRequest(request, userEntity);

        UserEntity updatedUser = userRepository.save(Objects.requireNonNull(userEntity));
        log.info("User updated profile");
        return userMapper.toUserDetailResponse(updatedUser);
    }

    @Override
    @CacheEvict(value = "user_details", key = "#username")
    public String updateAvatar(String username, MultipartFile avatarFile) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));
        String oldAvatar = userEntity.getAvatar();
        String newUrl = storageService.uploadAvatar(avatarFile);

        if (oldAvatar != null) {
            try {
                storageService.delete(oldAvatar, "avatars");
            } catch (Exception e) {
                log.warn("Failed to delete old image, but continuing update");
            }
        }
        userEntity.setAvatar(newUrl);
        log.info("User update avatar");
        return userRepository.save(userEntity).getAvatar();
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
    public void initiateEmailChange(String username, String newEmail, String currentPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException(Translator.tolocale("error.user.pw_incorrect"));
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new ResourceConflictException(Translator.tolocale("error.user.email_exists"));
        }

        generateAndSenResponseToken(user, OtpType.EMAIL_CHANGE, newEmail, newEmail);

        log.info("Email change initiated for user");
    }

    @Override
    @Transactional
    public void initiatePhoneChange(String username, String newPhone, String currentPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException(Translator.tolocale("error.user.pw_incorrect"));
        }

        generateAndSenResponseToken(user, OtpType.PHONE_CHANGE, newPhone, user.getEmail());

        log.info("Phone change initiated for user");
    }

    @Override
    @Transactional
    public UserDetailResponse addAddress(String username, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));
        ;
        AddressEntity newAddress = userMapper.toAddressEntity(request);

        user.addAddress(newAddress);

        userRepository.save(user);
        log.info("Add address for user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#username")
    public UserDetailResponse updateAddress(String username, Long addressId, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        AddressEntity addressToUpdate = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException(Translator.tolocale("error.user.address_not_owned")));

        userMapper.updateAddressFromRequest(request, addressToUpdate);

        userRepository.save(user);
        log.info("Update address for user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#username")
    public UserDetailResponse deleteAddress(String username, Long addressId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        AddressEntity addressToDelete = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.address_not_found")));

        user.removeAddress(addressToDelete);

        userRepository.save(user);
        log.info("Delete address for user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        log.info("Get all address for user");
        return user.getAddresses().stream()
                .map(userMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(String username, Long addressId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        AddressEntity address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(
                        () -> new AccessForbiddenException(Translator.tolocale("error.user.address_not_owned")));
        log.info("Get address for user");
        return userMapper.toAddressResponse(address);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#username")
    public void deleteUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));
        boolean hasChatHistory = messageRepository.existsBySenderOrRecipient(username);

        if (hasChatHistory) {
            userEntity.setUserStatus(UserStatus.INACTIVE);
            userEntity.setOnline(false);
            userEntity.setEmail(null);
            userEntity.setPhone(null);
            userEntity.setFirstName(Translator.tolocale("sys.account"));
            userEntity.setLastName(Translator.tolocale("sys.deleted"));
            userEntity.setAvatar(null);
            userRepository.save(userEntity);
            log.info("Soft deleted user: {} (user has message history)", username);
        } else {
            userRepository.delete(Objects.requireNonNull(userEntity));
            log.info("Hard deleted user: {} (user had no message history)", username);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#username")
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Translator.tolocale("error.user.not_found_with", username)));

        if (!passwordEncoder.matches(currentPassword, userEntity.getPassword())) {
            throw new InvalidPasswordException(Translator.tolocale("error.user.current_pw_incorrect"));
        }

        if (passwordEncoder.matches(newPassword, userEntity.getPassword())) {
            throw new PasswordMismatchException(Translator.tolocale("error.user.new_pw_same"));
        }

        userEntity.setPassword(passwordEncoder.encode(newPassword));

        int currentVersion = userEntity.getTokenVersion() == null ? 0 : userEntity.getTokenVersion();
        userEntity.setTokenVersion(currentVersion + 1);

        userRepository.save(userEntity);
        log.info("User {} changed password successfully", username);
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @CacheEvict(value = "user_details", key = "#username")
    @Async
    public void setUserOnlineStatus(String username, boolean isOnline) {
        userRepository.updateOnlineStatus(username, isOnline);
        log.info("Set user online status");
    }

    @Override
    @Transactional
    public void verifyEmailChange(String username, String otp) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        String oldEmail = user.getEmail();
        String newEmail = validateRedisOtp(username, OtpType.EMAIL_CHANGE, otp);

        if (newEmail == null || newEmail.isEmpty()) {
            throw new InvalidDataException(Translator.tolocale("error.user.invalid_new_email"));
        }

        user.setEmail(newEmail);
        userRepository.save(user);

        cuckooFilterService.delete(EMAIL_FILTER_KEY, oldEmail);
        cuckooFilterService.add(EMAIL_FILTER_KEY, newEmail);
        log.info("Email changed successfully via Redis OTP for user: {}", username);
    }

    @Override
    @Transactional
    public void verifyPhoneChange(String username, String otp) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        String newPhone = validateRedisOtp(username, OtpType.PHONE_CHANGE, otp);

        if (newPhone == null || newPhone.isEmpty()) {
            throw new InvalidDataException(Translator.tolocale("error.user.invalid_new_phone"));
        }

        user.setPhone(newPhone);
        userRepository.save(user);
        log.info("Phone changed successfully via Redis OTP for user: {}", username);
    }

    @Override
    @Transactional
    public void resendEmailChangeOtp(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        String redisKey = "otp:" + OtpType.EMAIL_CHANGE.name() + ":" + user.getUsername();
        String oldValue = (String) redisTemplate.opsForValue().get(redisKey);
        if (oldValue == null)
            throw new ResourceNotFoundException(Translator.tolocale("error.user.req_not_found"));

        String[] parts = oldValue.split(":");
        String newEmail = parts.length > 1 ? parts[1] : null;

        if (newEmail == null)
            throw new InvalidDataException(Translator.tolocale("error.user.missing_new_email"));

        resendRedisOtp(username, OtpType.EMAIL_CHANGE, newEmail);
    }

    @Override
    @Transactional
    public void resendPhoneChangeOtp(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found")));

        resendRedisOtp(username, OtpType.PHONE_CHANGE, user.getEmail());
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
        emailService.sendOtpEmail(emailToSend, usernameForMail, newOtp);
        log.info("Resent {} OTP to {}", type, emailToSend);
    }

}
