package com.web.backend.service.impl;

import com.web.backend.common.OtpType;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.*;
import com.web.backend.controller.response.*;
import com.web.backend.exception.*;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.*;
import com.web.backend.controller.response.UserDetailResponse;
import com.web.backend.repository.PendingUserRepository;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.repository.VerificationTokenRepository;
import com.web.backend.service.EmailService;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final MessageService messageService;

    private final EmailService emailService;

    private final UserMapper userMapper;

    private final PendingUserRepository pendingUserRepository;

    private final VerificationTokenRepository tokenRepository;

    private final RoleRepository roleRepository;

    @Value("${spring.sendgrid.expiration-minutes}")
    private int expirationMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(CreateUserRequest createUserRequest) {

        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            throw new ResourceConflictException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new ResourceConflictException("Email đã được sử dụng");
        }

        PendingUserEntity pendingUser = pendingUserRepository.findByEmail(createUserRequest.getEmail())
                .orElse(new PendingUserEntity());

        pendingUser.setUsername(createUserRequest.getUsername());
        pendingUser.setEmail(createUserRequest.getEmail());
        pendingUser.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        RoleEntity defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role mặc định 'USER' chưa được khởi tạo trong DB"));

        pendingUser.setRoleId(defaultRole.getId());

        SecureRandom secureRandom = new SecureRandom();
        int otpValue = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpValue);
        pendingUser.setOtpCode(otp);
        pendingUser.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));

        pendingUserRepository.save(pendingUser);

        emailService.sendOtpEmail(pendingUser.getEmail(), pendingUser.getUsername(), otp);

        log.info("Registering new user: {}", createUserRequest.getUsername());
        return UserResponse.builder()
                .username(pendingUser.getUsername())
                .email(pendingUser.getEmail())
                .userStatus(UserStatus.UNVERIFIED)
                .build();
    }

    @Override
    public OnlineUsersResponse getOnlineUsers() {
        List<UserEntity> userEntities = userRepository.findByIsOnlineTrue();
        Map<String, UserSummaryResponse> list = userEntities.stream()
                .collect(Collectors.toMap(
                        UserEntity::getUsername,
                        userMapper::toUserSummaryResponse
                ));
        log.info("Get online users");
        return OnlineUsersResponse.builder()
                .users(list)
                .build();
    }

    @Override
    public UserResponse getCurrentUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (user.getUserStatus() == UserStatus.INACTIVE) {
            throw new ResourceNotFoundException("Người dùng không tồn tại: " + username);
        }

        log.info("Get current user");
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserDetailResponse getProfileUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (user.getUserStatus() == UserStatus.INACTIVE) {
            throw new ResourceNotFoundException("Người dùng không tồn tại: " + username);
        }

        log.info("Get profile user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    public String getPublicKey(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
        log.info("Get public key");
        return userEntity.getPublicKey();
    }

    @Override
    public void savePublicKey(String username, String publicKey) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
        userEntity.setPublicKey(publicKey);
        userRepository.save(userEntity);
        log.info("Saved public key for user");
    }

    @Override
    @Transactional
    public UserDetailResponse updateUser(String username, UpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        userMapper.updateUserFromRequest(request, userEntity);

        UserEntity updatedUser = userRepository.save(userEntity);
        log.info("User updated profile");
        return userMapper.toUserDetailResponse(updatedUser);
    }

    private void generateAndSenResponseToken(UserEntity user, OtpType type, String extraData, String targetEmail) {
        tokenRepository.findByUserAndType(user, type).ifPresent(tokenRepository::delete);

        SecureRandom secureRandom = new SecureRandom();
        int otpValue = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpValue);

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(otp)
                .type(type)
                .extraData(extraData)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();

        tokenRepository.save(token);

        emailService.sendOtpEmail(targetEmail, user.getUsername(), otp);
    }

    @Override
    @Transactional
    public void initiateEmailChange(String username, String newEmail, String currentPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Mật khẩu không chính xác");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new ResourceConflictException("Email đã tồn tại");
        }

        generateAndSenResponseToken(user, OtpType.EMAIL_CHANGE, newEmail, newEmail);

        log.info("Email change initiated for user");
    }

    @Override
    @Transactional
    public void initiateForgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại trong hệ thống"));

        generateAndSenResponseToken(user, OtpType.PASSWORD_RESET, null, user.getEmail());

        log.info("Password reset initiated for email: {}", email);
    }

    @Override
    @Transactional
    public void initiatePhoneChange(String username, String newPhone, String currentPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Mật khẩu không chính xác");
        }

        generateAndSenResponseToken(user, OtpType.PHONE_CHANGE, newPhone, user.getEmail());

        log.info("Phone change initiated for user");
    }

    @Override
    @Transactional
    public UserDetailResponse addAddress(String username, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));;
        AddressEntity newAddress = userMapper.toAddressEntity(request);

        user.addAddress(newAddress);

        userRepository.save(user);
        log.info("Add address for user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse updateAddress(String username, Long addressId, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        AddressEntity addressToUpdate = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc sở hữu của bạn"));

        userMapper.updateAddressFromRequest(request, addressToUpdate);

        userRepository.save(user);
        log.info("Update address for user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse deleteAddress(String username, Long addressId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        AddressEntity addressToDelete = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        user.removeAddress(addressToDelete);

        userRepository.save(user);
        log.info("Delete address for user");
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        log.info("Get all address for user");
        return user.getAddresses().stream()
                .map(userMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(String username, Long addressId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        AddressEntity address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new AccessForbiddenException("Địa chỉ không tồn tại hoặc không thuộc sở hữu của bạn"));
        log.info("Get address for user");
        return userMapper.toAddressResponse(address);
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
        boolean hasChatHistory = messageService.hasMessages(username);

        if (hasChatHistory) {
            userEntity.setUserStatus(UserStatus.INACTIVE);
            userEntity.setOnline(false);
            userEntity.setEmail(null);
            userEntity.setPhone(null);
            userRepository.save(userEntity);
            log.info("Soft deleted user: {} (user has message history)", username);
        } else {
            userRepository.delete(userEntity);
            log.info("Hard deleted user: {} (user had no message history)", username);
        }
    }

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (!passwordEncoder.matches(currentPassword, userEntity.getPassword())) {
              throw  new InvalidPasswordException("Mật khẩu hiện tại không chính xác!");
        }

        if (passwordEncoder.matches(newPassword, userEntity.getPassword())) {
            throw new PasswordMismatchException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);
        log.info("User {} changed password successfully", username);
    }

    @Override
    public PageResponse<UserSummaryResponse> getAllUsers(int pageNo, int pageSize, String sortBy) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "id"));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Page<UserEntity> pageResult;

        pageResult = userRepository.findAllByUserStatusNot(UserStatus.INACTIVE, pageable);

        List<UserSummaryResponse> content = pageResult.getContent().stream()
                .map(userMapper::toUserSummaryResponse)
                .collect(Collectors.toList());

        log.info("Get all user");
        return PageResponse.<UserSummaryResponse>builder()
                .content(content)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public UserDetailResponse getUserByUsername(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (userEntity.getUserStatus() == UserStatus.INACTIVE) {
            throw new ResourceNotFoundException("Người dùng không tồn tại: " + username);
        }
        log.info("Get user");
        return userMapper.toUserDetailResponse(userEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse adminCreateUser(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceConflictException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email đã tồn tại trong hệ thống" + request.getEmail());
        }

        UserEntity user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (user.getUserStatus() == null) {
            user.setUserStatus(UserStatus.ACTIVE);
        }

        Long roleId = request.getRoleId();
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role " + roleId + " không tồn tại"));

        user.setRole(role);
        UserEntity savedUser = userRepository.save(user);
        log.info("Created new user: {}", savedUser.getUsername());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse lockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        userEntity.setUserStatus(UserStatus.LOCKED);
        userEntity.setOnline(false);

        UserEntity savedUser = userRepository.save(userEntity);
        log.info("Locked user: {}", username);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse unlockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (userEntity.getUserStatus() == UserStatus.LOCKED) {
            userEntity.setUserStatus(UserStatus.ACTIVE);
            UserEntity savedUser = userRepository.save(userEntity);
            log.info("Unlocked user: {}", username);

            return userMapper.toUserResponse(savedUser);
        }

        log.warn("Tried to unlock user {} who was not LOCKED (Status: {})", username, userEntity.getUserStatus());

        return userMapper.toUserResponse(userEntity);
    }

    @Override
    @Transactional
    public UserResponse adminUpdateUser(String username, AdminUpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (request.getEmail() != null && !request.getEmail().equals(userEntity.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResourceConflictException("Email đã tồn tại trong hệ thống");
            }
        }

        userMapper.updateAdminUserFromRequest(request, userEntity);
        if (request.getRoleId() != null) {
            RoleEntity newRole = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));
            userEntity.setRole(newRole);
        }

        UserEntity saved = userRepository.save(userEntity);
        log.info("Update user");
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public void adminDeleteUser(String targetUsername, String requesterUsername) {
        this.deleteUser(targetUsername);
        log.info("Delete user");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> adminGetAllAddresses(String targetUsername) {
        UserEntity user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng mục tiêu không tồn tại: " + targetUsername));

        log.info("Get all address for user");
        return user.getAddresses().stream()
                .map(userMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse adminGetAddressById(String targetUsername, Long addressId) {
        UserEntity user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng mục tiêu không tồn tại: " + targetUsername));

        AddressEntity address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new AccessForbiddenException("Địa chỉ không tồn tại hoặc không thuộc sở hữu của người dùng mục tiêu."));

        log.info("Get address with id for user");
        return userMapper.toAddressResponse(address);
    }

    @Override
    @Transactional
    public UserDetailResponse adminUpdateAddress(String targetUsername, Long addressId, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng mục tiêu không tồn tại: " + targetUsername));

        AddressEntity addressToUpdate = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc sở hữu của người dùng mục tiêu."));

        userMapper.updateAddressFromRequest(request, addressToUpdate);

        userRepository.save(user);
        log.info("Admin updated address {} for user: {}", addressId, targetUsername);
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public void adminDeleteAddress(String targetUsername, Long addressId) {
        UserEntity user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng mục tiêu không tồn tại: " + targetUsername));

        AddressEntity addressToDelete = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        user.removeAddress(addressToDelete);

        userRepository.save(user);
        log.info("Admin deleted address {} for user: {}", addressId, targetUsername);
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public void setUserOnlineStatus(String username, boolean isOnline) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            UserEntity userEntity = userOpt.get();
            userEntity.setOnline(isOnline);
            userRepository.save(userEntity);
        }
        log.info("Set user online status");
    }
}
