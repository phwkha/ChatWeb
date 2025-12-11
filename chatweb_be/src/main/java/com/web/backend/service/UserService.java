package com.web.backend.service;

import com.web.backend.controller.request.*;
import com.web.backend.controller.response.*;
import com.web.backend.controller.response.AddressResponse;
import com.web.backend.model.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findById(Long id);

    void setUserOnlineStatus(String username, boolean isOnline);

    boolean userExists(String username);

    boolean userExistsByEmail(String email);

    UserResponse createUser(CreateUserRequest createUserRequest);

    OnlineUsersResponse getOnlineUsers();

    UserResponse getCurrentUser(String username);

    UserDetailResponse getProfileUser(String username);

    void savePublicKey(String username, String publicKey);

    String getPublicKey(String username);

    UserDetailResponse updateUser(String username, UpdateUserRequest request);

    void initiateEmailChange(String username, String newEmail, String currentPassword);

    void initiateForgotPassword(String email);

    void initiatePhoneChange(String username, String newPhone, String currentPassword);

    UserDetailResponse addAddress(String username, AddressRequest request);

    UserDetailResponse updateAddress(String username, Long addressId, AddressRequest request);

    UserDetailResponse deleteAddress(String username, Long addressId);

    List<AddressResponse> getAllAddresses(String username);

    AddressResponse getAddressById(String username, Long addressId);

    void deleteUser(String username);

    void changePassword(String username, String currentPassword, String newPassword);

    PageResponse<UserSummaryResponse> getAllUsers(int pageNo, int pageSize, String sortBy);

    UserDetailResponse getUserByUsername(String username);

    UserResponse adminCreateUser(AdminCreateUserRequest request);

    UserResponse lockUser(String username);

    UserResponse unlockUser(String username);

    UserResponse adminUpdateUser(String username, AdminUpdateUserRequest request);

    void adminDeleteUser(String targetUsername, String requesterUsername);

    List<AddressResponse> adminGetAllAddresses(String targetUsername);

    AddressResponse adminGetAddressById(String targetUsername, Long addressId);

    UserDetailResponse adminUpdateAddress(String targetUsername, Long addressId, AddressRequest request);

    void adminDeleteAddress(String targetUsername, Long addressId);
}
