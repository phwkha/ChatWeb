package com.web.backend.controller;

import com.web.backend.controller.request.*;
import com.web.backend.controller.response.*;
import com.web.backend.controller.response.AddressResponse;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j(topic = "USER-CONTROLLER")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get current user: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy thông tin người dùng thành công", userService.getCurrentUser(userEntityPrincipal.getUsername())));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getProfileUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get profile user: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Lấy thông tin người dùng thành công",
                userService.getProfileUser(userEntityPrincipal.getUsername())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @RequestBody @Valid UpdateUserRequest updateUserRequest,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        String username = userEntityPrincipal.getUsername();

        log.info("Updating profile for user: {}", username);

        UserDetailResponse updatedUser = userService.updateUser(username, updateUserRequest);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật hồ sơ thành công", updatedUser));
    }

    @PatchMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> updateAvatar(
            @RequestParam("file") MultipartFile avatarFile,
            Authentication authentication) {

        UserEntity userEntity = (UserEntity) authentication.getPrincipal();

        String urlAvatar = userService.updateAvatar(userEntity.getUsername(), avatarFile);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật ảnh đại diện thành công", urlAvatar));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("User {} changed password successfully", userEntityPrincipal.getUsername());
        userService.changePassword(userEntityPrincipal.getUsername(), request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Đổi mật khẩu thành công", null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();

        String username = userEntityPrincipal.getUsername();

        log.warn("User {} is deleting their account", username);
        userService.deleteUser(username);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Xóa tài khoản thành công", null));
    }

    @PostMapping("/address")
    public ResponseEntity<ApiResponse<UserDetailResponse>> addAddress(
            Authentication authentication,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Adding address for user: {}", currentUser.getUsername());
        UserDetailResponse result = userService.addAddress(currentUser.getUsername(), addressRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Thêm địa chỉ mới thành công", result));
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Updating address {} for user {}", addressId, currentUser.getUsername());
        UserDetailResponse result = userService.updateAddress(currentUser.getUsername(), addressId, addressRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật địa chỉ thành công", result));
    }

    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Deleting address {} for user {}", addressId, currentUser.getUsername());
        UserDetailResponse result = userService.deleteAddress(currentUser.getUsername(), addressId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Xóa địa chỉ thành công", result));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses(Authentication authentication) {
        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Get all address for user: {}", currentUser.getUsername());
        List<AddressResponse> addresses = userService.getAllAddresses(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy danh sách địa chỉ thành công", addresses));
    }

    @GetMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressDetail(
            Authentication authentication,
            @PathVariable Long addressId) {
        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Get address for user: {}", currentUser.getUsername());
        AddressResponse address = userService.getAddressById(currentUser.getUsername(), addressId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy chi tiết địa chỉ thành công", address));
    }

    @PostMapping("/initiate-email-change")
    public ResponseEntity<ApiResponse<Void>> initiateEmailChange(
            Authentication authentication,
            @RequestBody @Valid InitiateEmailChangeRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Email change initiated for user: {}", user.getUsername());
        userService.initiateEmailChange(user.getUsername(), request.getNewEmail(), request.getCurrentPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Mã xác thực đã được gửi đến email mới: " + request.getNewEmail(), null));
    }

    @PostMapping("/verify-email-change")
    public ResponseEntity<ApiResponse<Void>> verifyEmailChange(
            Authentication authentication,
            @RequestBody @Valid VerifyOtpRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Email changed successfully for user: {}", user.getUsername());
        userService.verifyEmailChange(user.getUsername(), request.getOtp());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật email thành công!", null));
    }

    @PostMapping("/resend-email-verification")
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Resent Email Change OTP for user {}", user.getUsername());
        userService.resendEmailChangeOtp(user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Đã gửi lại mã xác thực vào email mới.", null));
    }

    @PostMapping("/initiate-phone-change")
    public ResponseEntity<ApiResponse<Void>> initiatePhoneChange(
            Authentication authentication,
            @RequestBody @Valid InitiatePhoneChangeRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Phone change initiated for user: {}", user.getUsername());
        userService.initiatePhoneChange(user.getUsername(), request.getNewPhone(), request.getCurrentPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Mã xác thực đã được gửi để xác nhận đổi số điện thoại.", null));
    }

    @PostMapping("/verify-phone-change")
    public ResponseEntity<ApiResponse<Void>> verifyPhoneChange(
            Authentication authentication,
            @RequestBody @Valid VerifyOtpRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Phone changed successfully for user: {}", user.getUsername());
        userService.verifyPhoneChange(user.getUsername(), request.getOtp());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Cập nhật số điện thoại thành công!", null));
    }

    @PostMapping("/resend-phone-change-verification")
    public ResponseEntity<ApiResponse<Void>> resendPhoneVerification(Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();

        userService.resendPhoneChangeOtp(user.getUsername());
        log.info("Resent Phone Change OTP for user: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Đã gửi lại mã xác thực đổi số điện thoại.", null));
    }

}