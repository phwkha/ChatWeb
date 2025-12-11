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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        PageResponse<UserSummaryResponse> users = userService.getAllUsers(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy danh sách user thành công", users));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserByUsername(@PathVariable String username) {
        UserDetailResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy thông tin user thành công", user));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<UserResponse>> addUser(@RequestBody @Valid AdminCreateUserRequest request) {
        UserResponse newUser = userService.adminCreateUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Tạo user thành công", newUser));
    }

    @PostMapping("/{username}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable String username) {
        UserResponse unlockedUser = userService.unlockUser(username);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Mở khóa user thành công", unlockedUser));
    }

    @PostMapping("/{username}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(@PathVariable String username) {
        UserResponse lockedUser = userService.lockUser(username);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Khóa user thành công", lockedUser));
    }

    @PutMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String username,
            @RequestBody @Valid AdminUpdateUserRequest request) {
        UserResponse updatedUser = userService.adminUpdateUser(username, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật user thành công", updatedUser));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String username, Authentication authentication) {
        UserEntity adminPrincipal = (UserEntity) authentication.getPrincipal();

        userService.adminDeleteUser(username, adminPrincipal.getUsername());

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(HttpStatus.NO_CONTENT.value(), "Xóa user thành công", null));
    }

    @GetMapping("/user/{username}/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddressesForUser(@PathVariable String username) {
        List<AddressResponse> addresses = userService.adminGetAllAddresses(username);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy tất cả địa chỉ của người dùng " + username + " thành công",
                addresses));
    }

    @GetMapping("/user/{username}/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressByIdForUser(
            @PathVariable String username,
            @PathVariable Long addressId) {

        AddressResponse address = userService.adminGetAddressById(username, addressId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy chi tiết địa chỉ thành công",
                address));
    }

    @PutMapping("/user/{username}/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateAddressForUser(
            @PathVariable String username,
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserDetailResponse result = userService.adminUpdateAddress(username, addressId, addressRequest);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Admin đã cập nhật địa chỉ thành công cho người dùng " + username,
                result));
    }

    @DeleteMapping("/user/{username}/address/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddressForUser(
            @PathVariable String username,
            @PathVariable Long addressId) {

        userService.adminDeleteAddress(username, addressId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(
                        HttpStatus.NO_CONTENT.value(),
                        "Admin đã xóa địa chỉ thành công cho người dùng " + username,
                        null));
    }
}