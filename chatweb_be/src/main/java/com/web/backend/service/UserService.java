package com.web.backend.service;

import com.web.backend.controller.request.AdminCreateUserRequest;
import com.web.backend.controller.request.AdminUpdateUserRequest;
import com.web.backend.controller.request.CreateUserRequest;
import com.web.backend.controller.request.UpdateUserRequest;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {

    UserDTO addUser(CreateUserRequest createUserRequest);

    Optional<UserEntity> findByUsername(String username);

    Map<String, Object> getOnlineUsers();

    boolean userExists(String username);

    void setUserOnlineStatus(String username, boolean isOnline);

    Optional<UserEntity> findById(Long id);

    void savePublicKey(String username, String publicKey);

    String getPublicKey(String username);

    UserDTO getUserByUsername(String username);

    List<UserDTO> getAllUsers();

    UserDTO updateUser(String username, UpdateUserRequest request);

    void deleteUser(String username);

    UserDTO lockUser(String username);

    UserDTO unlockUser(String username);

    UserDTO adminCreateUser(AdminCreateUserRequest request);

    UserDTO adminUpdateUser(String username, AdminUpdateUserRequest request);

    void changePassword(String username, String currentPassword, String newPassword);
}
