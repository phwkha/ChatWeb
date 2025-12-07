package com.web.backend.service;

import com.web.backend.controller.request.*;
import com.web.backend.controller.response.OnlineUsersResponse;
import com.web.backend.controller.response.PageResponse;
import com.web.backend.model.DTO.AddressDTO;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findById(Long id);

    void setUserOnlineStatus(String username, boolean isOnline);

    boolean userExists(String username);

    boolean userExistsByEmail(String email);

    UserDTO createUser(CreateUserRequest createUserRequest);

    OnlineUsersResponse getOnlineUsers();

    UserDTO getCurrentUser(String username);

    void savePublicKey(String username, String publicKey);

    String getPublicKey(String username);

    UserDTO updateUser(String username, UpdateUserRequest request);

    UserDTO addAddress(String username, AddressRequest request);

    UserDTO updateAddress(String username, Long addressId, AddressRequest request);

    UserDTO deleteAddress(String username, Long addressId);

    List<AddressDTO> getAllAddresses(String username);

    AddressDTO getAddressById(String username, Long addressId);

    void deleteUser(String username);

    void changePassword(String username, String currentPassword, String newPassword);

    PageResponse<UserDTO> getAllUsers(int pageNo, int pageSize, String sortBy);

    UserDTO getUserByUsername(String username);

    UserDTO adminCreateUser(AdminCreateUserRequest request);

    UserDTO lockUser(String username);

    UserDTO unlockUser(String username);

    UserDTO adminUpdateUser(String username, AdminUpdateUserRequest request);

    void adminDeleteUser(String targetUsername, String requesterUsername);

    List<AddressDTO> adminGetAllAddresses(String targetUsername);

    AddressDTO adminGetAddressById(String targetUsername, Long addressId);

    UserDTO adminUpdateAddress(String targetUsername, Long addressId, AddressRequest request);

    UserDTO adminDeleteAddress(String targetUsername, Long addressId);
}
