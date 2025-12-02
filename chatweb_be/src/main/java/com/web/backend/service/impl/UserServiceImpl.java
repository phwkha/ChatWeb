package com.web.backend.service.impl;

import com.web.backend.common.Convert;
import com.web.backend.common.Role;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.AdminCreateUserRequest;
import com.web.backend.controller.request.AdminUpdateUserRequest;
import com.web.backend.controller.request.CreateUserRequest;
import com.web.backend.controller.request.UpdateUserRequest;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO addUser(CreateUserRequest createUserRequest) {
        // Sử dụng Role.USER mặc định
        UserEntity userEntity = createNewUserEntity(
                createUserRequest.getUsername(),
                createUserRequest.getPassword(),
                createUserRequest.getEmail(),
                Role.USER
        );

        UserEntity savedUserEntity = userRepository.save(userEntity);
        log.info("New user registered: {}", savedUserEntity.getUsername());
        return Convert.UserConvertToUserDTO(savedUserEntity);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);

        // Nếu tìm thấy user nhưng trạng thái là INACTIVE (đã xóa) -> Coi như không tìm thấy
        if (userOpt.isPresent() && userOpt.get().getUserStatus() == UserStatus.INACTIVE) {
            return Optional.empty();
        }

        return userOpt;
    }

    @Override
    public Map<String, Object> getOnlineUsers() {
        List<UserEntity> userEntities = userRepository.findByIsOnlineTrue();
        Map<String, Object> map = userEntities.stream().collect(Collectors.toMap(UserEntity::getUsername, user -> user));
        return map;
    }

    @Override
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
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public void savePublicKey(String username, String publicKey) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        userEntity.setPublicKey(publicKey);
        userRepository.save(userEntity);
        log.info("Saved public key for user: {}", username);
    }

    @Override
    public String getPublicKey(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return userEntity.getPublicKey();
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Thêm kiểm tra này:
        if (userEntity.getUserStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("User not found: " + username);
        }

        return Convert.UserConvertToUserDTO(userEntity);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAllByUserStatusNot(UserStatus.INACTIVE).stream()
                .map(Convert::UserConvertToUserDTO) // Sử dụng hàm convert của bạn
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateUser(String username, UpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (request.getFullName() != null) {
            if (request.getFullName().trim().isEmpty()) {
                userEntity.setFullName(null);
            }  else {
                userEntity.setFullName(request.getFullName());
            }
        }
        if (request.getEmail() != null) {
            if (request.getEmail().trim().isEmpty()) {
                userEntity.setEmail(null);
            } else {
                userEntity.setEmail(request.getEmail());
            }
        }
        if (request.getPhone() != null) {
            if (request.getPhone().trim().isEmpty()) {
                userEntity.setPhone(null); // Lưu null để tránh lỗi unique
            } else {
                userEntity.setPhone(request.getPhone());
            }
        }

        UserEntity updatedUser = userRepository.save(userEntity);
        log.info("User updated: {}", updatedUser.getUsername());
        return Convert.UserConvertToUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        boolean hasChatHistory = messageService.hasMessages(username);

        if (hasChatHistory) {
            // Cách 1: Xóa mềm (Soft Delete)
            userEntity.setUserStatus(UserStatus.INACTIVE);
            userEntity.setOnline(false); // Đảm bảo trạng thái online là false
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
    public UserDTO lockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        userEntity.setUserStatus(UserStatus.LOCKED); // Đặt trạng thái LÓCKED
        userEntity.setOnline(false); // Buộc offline
        UserEntity savedUser = userRepository.save(userEntity);

        log.info("Admin locked user: {}", username);
        return Convert.UserConvertToUserDTO(savedUser);
    }

    @Override
    @Transactional
    public UserDTO unlockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        if (userEntity.getUserStatus() == UserStatus.LOCKED) {
            userEntity.setUserStatus(UserStatus.ACTIVE);
            UserEntity savedUser = userRepository.save(userEntity);
            log.info("Admin unlocked user: {}", username);
            return Convert.UserConvertToUserDTO(savedUser);
        }

        log.warn("Admin tried to unlock user {} who was not LOCKED (Status: {})", username, userEntity.getUserStatus());
        return Convert.UserConvertToUserDTO(userEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO adminCreateUser(AdminCreateUserRequest request) {
        // Sử dụng Role từ request
        UserEntity userEntity = createNewUserEntity(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getRole()
        );

        UserEntity savedUserEntity = userRepository.save(userEntity);
        log.info("Admin created new user: {} with role: {}", savedUserEntity.getUsername(), savedUserEntity.getRole());
        return Convert.UserConvertToUserDTO(savedUserEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO adminUpdateUser(String username, AdminUpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Cập nhật các trường nếu chúng được cung cấp (không phải null)
        if (request.getFullName() != null) {
            if (request.getFullName().trim().isEmpty()) {
                userEntity.setFullName(null);
            }  else {
                userEntity.setFullName(request.getFullName());
            }
        }
        if (request.getEmail() != null) {
            if (request.getEmail().trim().isEmpty()) {
                userEntity.setEmail(null);
            } else {
                userEntity.setEmail(request.getEmail());
            }
        }
        if (request.getPhone() != null) {
            if (request.getPhone().trim().isEmpty()) {
                userEntity.setPhone(null);
            } else {
                userEntity.setPhone(request.getPhone());
            }
        }
        if ((request.getRole() != null) && (request.getRole() != Role.ADMIN_PRO)) {
            userEntity.setRole(request.getRole());
        }

        UserEntity updatedUser = userRepository.save(userEntity);
        return Convert.UserConvertToUserDTO(updatedUser);
    }

    // Thêm vào UserServiceImpl
    private UserEntity createNewUserEntity(String username, String password, String email, Role role) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity.setEmail(email);
        userEntity.setOnline(false);
        userEntity.setRole(role);
        userEntity.setUserStatus(UserStatus.ACTIVE);
        return userEntity;
    }

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, userEntity.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
        }

        if (passwordEncoder.matches(newPassword, userEntity.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);
        log.info("User {} changed password successfully", username);
    }

}
