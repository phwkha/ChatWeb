package com.web.backend.service.impl;

import com.web.backend.common.Role;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.*;
import com.web.backend.controller.response.OnlineUsersResponse;
import com.web.backend.controller.response.PageResponse;
import com.web.backend.exception.AccessForbiddenException;
import com.web.backend.exception.ResourceConflictException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.AddressEntity;
import com.web.backend.model.DTO.AddressDTO;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO createUser(CreateUserRequest createUserRequest) {
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            throw new ResourceConflictException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new ResourceConflictException("Email đã tồn tại");
        }

        UserEntity user = new UserEntity();
        user.setUsername(createUserRequest.getUsername());
        user.setEmail(createUserRequest.getEmail());
        user.setRole(Role.USER);
        user.setUserStatus(UserStatus.ACTIVE);

        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));

        UserEntity savedUserEntity = userRepository.save(user);
        log.info("Create: new user: {}", savedUserEntity.getUsername());
        return userMapper.toUserDTO(savedUserEntity);
    }

    @Override
    public OnlineUsersResponse getOnlineUsers() {
        List<UserEntity> userEntities = userRepository.findByIsOnlineTrue();
        Map<String, UserDTO> list = userEntities.stream()
                .collect(Collectors.toMap(
                        UserEntity::getUsername, userMapper::toUserDTO
                ));
        log.info("Get online: users: {}", list.keySet());
        return OnlineUsersResponse.builder()
                .users(list)
                .build();
    }

    @Override
    public UserDTO getCurrentUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (user.getUserStatus() == UserStatus.INACTIVE) {
            throw new ResourceNotFoundException("Người dùng không tồn tại: " + username);
        }

        log.info("Get current user: {}", user.getUsername());
        return userMapper.toUserDTO(user);
    }

    @Override
    public String getPublicKey(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
        log.info("Get public key: {}", userEntity.getPublicKey());
        return userEntity.getPublicKey();
    }

    @Override
    public void savePublicKey(String username, String publicKey) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
        userEntity.setPublicKey(publicKey);
        UserEntity user = userRepository.save(userEntity);
        log.info("Saved public key for user: {}", username);
    }

    @Override
    @Transactional
    public UserDTO updateUser(String username, UpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        // Refactor: Dùng Mapper update trực tiếp, sạch hơn cách viết if-else thủ công
        userMapper.updateUserFromRequest(request, userEntity);

        UserEntity updatedUser = userRepository.save(userEntity);
        log.info("User updated profile: {}", updatedUser.getUsername());
        return userMapper.toUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public UserDTO addAddress(String username, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));;
        AddressEntity newAddress = userMapper.toAddressEntity(request);

        user.addAddress(newAddress);

        userRepository.save(user);
        log.info("Add address for user: {}", username);
        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateAddress(String username, Long addressId, AddressRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        // Tìm address trong list của user (đảm bảo user chỉ sửa địa chỉ của chính mình)
        AddressEntity addressToUpdate = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc sở hữu của bạn"));

        // Update data
        userMapper.updateAddressFromRequest(request, addressToUpdate);

        userRepository.save(user);
        log.info("Update address for user: {}", username);
        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional
    public UserDTO deleteAddress(String username, Long addressId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        AddressEntity addressToDelete = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        user.removeAddress(addressToDelete);

        userRepository.save(user);
        log.info("Delete address for user: {}", username);
        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAllAddresses(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        log.info("Get all address for user: {}", username);
        return user.getAddresses().stream()
                .map(userMapper::toAddressDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(String username, Long addressId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        AddressEntity address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new AccessForbiddenException("Địa chỉ không tồn tại hoặc không thuộc sở hữu của bạn"));
        log.info("Get address for user: {}", username);
        return userMapper.toAddressDTO(address);
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
        boolean hasChatHistory = messageService.hasMessages(username);

        if (hasChatHistory) {
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
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

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

    @Override
    public PageResponse<UserDTO> getAllUsers(int pageNo, int pageSize, String sortBy) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "id"));

        Page<UserEntity> pageResult = userRepository.findAllByUserStatusNot(UserStatus.INACTIVE, pageable);

        List<UserDTO> content = pageResult.getContent().stream()
                .map(userMapper::toUserDTO)
                .collect(Collectors.toList());

        return PageResponse.<UserDTO>builder()
                .content(content)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (userEntity.getUserStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("User not found: " + username);
        }

        return userMapper.toUserDTO(userEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO adminCreateUser(AdminCreateUserRequest request) {
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

        UserEntity savedUser = userRepository.save(user);
        log.info("Admin created new user: {} with role: {}", savedUser.getUsername(), savedUser.getRole());

        return userMapper.toUserDTO(savedUser);
    }

    @Override
    @Transactional
    public UserDTO lockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        userEntity.setUserStatus(UserStatus.LOCKED);
        userEntity.setOnline(false);

        UserEntity savedUser = userRepository.save(userEntity);
        log.info("Admin locked user: {}", username);

        return userMapper.toUserDTO(savedUser);
    }

    @Override
    @Transactional
    public UserDTO unlockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (userEntity.getUserStatus() == UserStatus.LOCKED) {
            userEntity.setUserStatus(UserStatus.ACTIVE);
            UserEntity savedUser = userRepository.save(userEntity);
            log.info("Admin unlocked user: {}", username);

            return userMapper.toUserDTO(savedUser);
        }

        log.warn("Admin tried to unlock user {} who was not LOCKED (Status: {})", username, userEntity.getUserStatus());

        return userMapper.toUserDTO(userEntity);
    }

    @Override
    @Transactional
    public UserDTO adminUpdateUser(String username, AdminUpdateUserRequest request) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (request.getEmail() != null && !request.getEmail().equals(userEntity.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResourceConflictException("Email đã tồn tại trong hệ thống");
            }
        }

        userMapper.updateAdminUserFromRequest(request, userEntity);

        UserEntity saved = userRepository.save(userEntity);
        return userMapper.toUserDTO(saved);
    }

    @Override
    @Transactional
    public void adminDeleteUser(String targetUsername, String requesterUsername) {
        UserEntity userToDelete = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + targetUsername));

        UserEntity requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng yêu cầu không tồn tại"));

        if (userToDelete.getRole() == Role.ADMIN_PRO) {
            throw new AccessForbiddenException("Không thể xóa tài khoản ADMIN_PRO");
        }

        if (userToDelete.getRole() == Role.ADMIN) {
            if (requester.getRole() != Role.ADMIN_PRO) {
                throw new AccessForbiddenException("Chỉ ADMIN_PRO mới có quyền xóa tài khoản ADMIN!");
            }
        }

        this.deleteUser(targetUsername);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getUserStatus() == UserStatus.INACTIVE) {
            return Optional.empty();
        }

        return userOpt;
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
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
}
