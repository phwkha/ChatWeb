package com.web.backend.controller;

import com.web.backend.controller.request.*;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.OnlineUsersResponse;
import com.web.backend.model.DTO.AddressDTO;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;
import com.web.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerUser(@RequestBody @Valid CreateUserRequest createUserRequest) {
        log.info("Registering new user: {}", createUserRequest.getUsername());

        UserDTO newUser = userService.createUser(createUserRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Đăng ký tài khoản thành công", newUser));
    }

    @GetMapping("/online")
    public ResponseEntity<ApiResponse<OnlineUsersResponse>> getOnlineUsers() {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy danh sách người dùng trực tuyến thành công", userService.getOnlineUsers()));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy thông tin người dùng thành công", userService.getCurrentUser(userEntityPrincipal.getUsername())));
    }

    @GetMapping("/public-key/{username}")
    public ResponseEntity<ApiResponse<String>> getPublicKey(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy khóa công khai thành công", userService.getPublicKey(username)));
    }

    @PostMapping("/public-key")
    public ResponseEntity<ApiResponse<Void>> savePublicKey(Authentication authentication, @RequestBody @Valid SavePublicKeyRequest request) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        userService.savePublicKey(userEntityPrincipal.getUsername(), request.getPublicKey());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lưu khóa công khai thành công", null));
    }

    @PutMapping("/{username}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable String username,
            @RequestBody @Valid UpdateUserRequest updateUserRequest,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();

        if (!userEntityPrincipal.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Bạn không có quyền chỉnh sửa hồ sơ này"));
        }

        log.info("Updating profile for user: {}", username);
        UserDTO updatedUser = userService.updateUser(username, updateUserRequest);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật hồ sơ thành công", updatedUser));
    }

    // --- Address Section ---

    @PostMapping("/address")
    public ResponseEntity<ApiResponse<UserDTO>> addAddress(
            Authentication authentication,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Adding address for user: {}", currentUser.getUsername());
        UserDTO result = userService.addAddress(currentUser.getUsername(), addressRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Thêm địa chỉ mới thành công", result));
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDTO>> updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Updating address {} for user {}", addressId, currentUser.getUsername());
        UserDTO result = userService.updateAddress(currentUser.getUsername(), addressId, addressRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật địa chỉ thành công", result));
    }

    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDTO>> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Deleting address {} for user {}", addressId, currentUser.getUsername());
        UserDTO result = userService.deleteAddress(currentUser.getUsername(), addressId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Xóa địa chỉ thành công", result));
    }

    @GetMapping("/address")
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getAllAddresses(Authentication authentication) {
        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        List<AddressDTO> addresses = userService.getAllAddresses(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy danh sách địa chỉ thành công", addresses));
    }

    @GetMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressDTO>> getAddressDetail(
            Authentication authentication,
            @PathVariable Long addressId) {
        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        AddressDTO address = userService.getAddressById(currentUser.getUsername(), addressId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy chi tiết địa chỉ thành công", address));
    }

    // --- Account Management ---

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String username, Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();

        if (!userEntityPrincipal.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Bạn chỉ có thể xóa tài khoản của chính mình"));
        }

        log.warn("User {} is deleting their account", username);
        userService.deleteUser(username);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Xóa tài khoản thành công", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        userService.changePassword(userEntityPrincipal.getUsername(), request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Đổi mật khẩu thành công", null));
    }
}