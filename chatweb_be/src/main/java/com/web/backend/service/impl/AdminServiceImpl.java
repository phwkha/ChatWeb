package com.web.backend.service.impl;

import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.AddressRequest;
import com.web.backend.controller.request.AdminCreateUserRequest;
import com.web.backend.controller.request.AdminUpdateUserRequest;
import com.web.backend.controller.response.*;
import com.web.backend.exception.AccessForbiddenException;
import com.web.backend.exception.ResourceConflictException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.AddressEntity;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.AdminService;
import com.web.backend.service.MessageService;
import com.web.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j(topic = "ADMIN-SERVICE")
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    private final MessageService messageService;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    private final RoleRepository roleRepository;

    private final StorageService storageService;

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ONLINE_USERS_KEY = "online_users";

    @Override
    public OnlineUsersResponse getOnlineUsers() {
        Set<Object> onlineUsernames = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);

        if (onlineUsernames == null || onlineUsernames.isEmpty()) {
            return OnlineUsersResponse.builder().users(Collections.emptyMap()).build();
        }

        List<String> usernames = onlineUsernames.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        List<UserEntity> userEntities = userRepository.findByUsernameIn(usernames);

        Map<String, UserSummaryResponse> list = userEntities.stream()
                .peek(entity -> entity.setOnline(true))
                .collect(Collectors.toMap(
                        UserEntity::getUsername,
                        userMapper::toUserSummaryResponse
                ));

        return OnlineUsersResponse.builder().users(list).build();
    }

    @Override
    public PageResponse<UserSummaryResponse> getAllUsers(int pageNo, int pageSize, String sortBy) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "id"));

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
    @CacheEvict(value = "user_details", key = "#username")
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
    @CacheEvict(value = "user_details", key = "#username")
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
    @CacheEvict(value = "user_details", key = "#username")
    public void deleteAvatar(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        String urlAvatar = userEntity.getAvatar();

        userEntity.setAvatar(null);

        storageService.delete(urlAvatar, "avatars");

        log.info("delete avatar for user {}", username);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#username")
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
    @CacheEvict(value = "user_details", key = "#targetUsername")
    public void adminDeleteUser(String targetUsername, String requesterUsername) {
        UserEntity userEntity = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + targetUsername));
        boolean hasChatHistory = messageService.hasMessages(targetUsername);

        if (hasChatHistory) {
            userEntity.setUserStatus(UserStatus.INACTIVE);
            userEntity.setOnline(false);
            userEntity.setEmail(null);
            userEntity.setPhone(null);
            userRepository.save(userEntity);
            log.info("Soft deleted user: {} (user has message history) by {}", targetUsername, requesterUsername);
        } else {
            userRepository.delete(userEntity);
            log.info("Hard deleted user: {} (user had no message history) by {}", targetUsername, requesterUsername);
        }
        log.info("Delete user");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> adminGetAllAddresses(String targetUsername) {
        UserEntity user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng mục tiêu không tồn tại: " + targetUsername));

        log.info("Get all address for user by admin");
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
    @CacheEvict(value = "user_details", key = "#username")
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
    @CacheEvict(value = "user_details", key = "#username")
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

}
