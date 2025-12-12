package com.web.backend.controller;

import com.web.backend.controller.request.AddressRequest;
import com.web.backend.controller.request.AdminCreateUserRequest;
import com.web.backend.controller.request.AdminUpdateUserRequest;
import com.web.backend.controller.response.*;
import com.web.backend.controller.response.AddressResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j(topic = "ADMIN-CONTROLLER")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getAllUsers(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get all user by: {}", userEntityPrincipal.getUsername());
        PageResponse<UserSummaryResponse> users = userService.getAllUsers(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy danh sách user thành công", users));
    }

    @GetMapping("/user/{username}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserByUsername(Authentication authentication, @PathVariable String username) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get user by: {}", userEntityPrincipal.getUsername());
        UserDetailResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy thông tin user thành công", user));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResponse<UserResponse>> addUser(Authentication authentication, @RequestBody @Valid AdminCreateUserRequest request) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Add user by: {}", userEntityPrincipal.getUsername());
        UserResponse newUser = userService.adminCreateUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Tạo user thành công", newUser));
    }

    @PostMapping("/{username}/unlock")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(Authentication authentication, @PathVariable String username) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Unlock user by: {}", userEntityPrincipal.getUsername());
        UserResponse unlockedUser = userService.unlockUser(username);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Mở khóa user thành công", unlockedUser));
    }

    @PostMapping("/{username}/lock")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(Authentication authentication, @PathVariable String username) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Lock user by: {}", userEntityPrincipal.getUsername());
        UserResponse lockedUser = userService.lockUser(username);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Khóa user thành công", lockedUser));
    }

    @PutMapping("/{username}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            Authentication authentication,
            @PathVariable String username,
            @RequestBody @Valid AdminUpdateUserRequest request) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Update user by: {}", userEntityPrincipal.getUsername());
        UserResponse updatedUser = userService.adminUpdateUser(username, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật user thành công", updatedUser));
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String username, Authentication authentication) {
        UserEntity adminPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Delete user by {}", adminPrincipal.getUsername());

        userService.adminDeleteUser(username, adminPrincipal.getUsername());

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(HttpStatus.NO_CONTENT.value(), "Xóa user thành công", null));
    }

    @GetMapping("/user/{username}/addresses")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddressesForUser(
            Authentication authentication,
            @PathVariable String username) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get all address for user by: {}", userEntityPrincipal.getUsername());
        List<AddressResponse> addresses = userService.adminGetAllAddresses(username);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy tất cả địa chỉ của người dùng " + username + " thành công",
                addresses));
    }

    @GetMapping("/user/{username}/address/{addressId}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressByIdForUser(
            Authentication authentication,
            @PathVariable String username,
            @PathVariable Long addressId) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get address with id for user by: {}", userEntityPrincipal.getUsername());

        AddressResponse address = userService.adminGetAddressById(username, addressId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy chi tiết địa chỉ thành công",
                address));
    }

    @PutMapping("/user/{username}/address/{addressId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateAddressForUser(
            Authentication authentication,
            @PathVariable String username,
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest addressRequest) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Update address for user by: {}", userEntityPrincipal.getUsername());

        UserDetailResponse result = userService.adminUpdateAddress(username, addressId, addressRequest);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Admin đã cập nhật địa chỉ thành công cho người dùng " + username,
                result));
    }

    @DeleteMapping("/user/{username}/address/{addressId}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteAddressForUser(
            Authentication authentication,
            @PathVariable String username,
            @PathVariable Long addressId) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Delete address for user by: {}", userEntityPrincipal.getUsername());

        userService.adminDeleteAddress(username, addressId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(
                        HttpStatus.NO_CONTENT.value(),
                        "Admin đã xóa địa chỉ thành công cho người dùng " + username,
                        null));
    }
}