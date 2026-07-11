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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.web.backend.config.LocalResolverConfig.Translator;

@Slf4j(topic = "USER-CONTROLLER")
@Tag(name = "User Controller")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user", description = "API endpoint for get current user")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get current user: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.get_info"), userService.getCurrentUser(userEntityPrincipal.getUsername())));
    }

    @Operation(summary = "Get profile user", description = "API endpoint for get profile user")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getProfileUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get profile user: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                Translator.tolocale("success.user.get_info"),
                userService.getProfileUser(userEntityPrincipal.getUsername())));
    }

    @Operation(summary = "Update user", description = "API endpoint for update user")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @RequestBody @Valid UpdateUserRequest updateUserRequest,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        String username = userEntityPrincipal.getUsername();

        log.info("Updating profile for user: {}", username);

        UserDetailResponse updatedUser = userService.updateUser(username, updateUserRequest);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.update_profile"), updatedUser));
    }

    @Operation(summary = "Update avatar", description = "API endpoint for update avatar")
    @PatchMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> updateAvatar(
            @RequestParam("file") MultipartFile avatarFile,
            Authentication authentication) {

        UserEntity userEntity = (UserEntity) authentication.getPrincipal();

        String urlAvatar = userService.updateAvatar(userEntity.getUsername(), avatarFile);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.update_avatar"), urlAvatar));
    }

    @Operation(summary = "Change password", description = "API endpoint for change password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("User {} changed password successfully", userEntityPrincipal.getUsername());
        userService.changePassword(userEntityPrincipal.getUsername(), request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.change_pwd"), null));
    }

    @Operation(summary = "Delete user", description = "API endpoint for delete user")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();

        String username = userEntityPrincipal.getUsername();

        log.warn("User {} is deleting their account", username);
        userService.deleteUser(username);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.del_account"), null));
    }

    @Operation(summary = "Add address", description = "API endpoint for add address")
    @PostMapping("/address")
    public ResponseEntity<ApiResponse<UserDetailResponse>> addAddress(
            Authentication authentication,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Adding address for user: {}", currentUser.getUsername());
        UserDetailResponse result = userService.addAddress(currentUser.getUsername(), addressRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), Translator.tolocale("success.user.add_address"), result));
    }

    @Operation(summary = "Update address", description = "API endpoint for update address")
    @PutMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest addressRequest) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Updating address {} for user {}", addressId, currentUser.getUsername());
        UserDetailResponse result = userService.updateAddress(currentUser.getUsername(), addressId, addressRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.update_address"), result));
    }

    @Operation(summary = "Delete address", description = "API endpoint for delete address")
    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Deleting address {} for user {}", addressId, currentUser.getUsername());
        UserDetailResponse result = userService.deleteAddress(currentUser.getUsername(), addressId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.del_address"), result));
    }

    @Operation(summary = "Get all addresses", description = "API endpoint for get all addresses")
    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses(Authentication authentication) {
        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Get all address for user: {}", currentUser.getUsername());
        List<AddressResponse> addresses = userService.getAllAddresses(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.get_addresses"), addresses));
    }

    @Operation(summary = "Get address detail", description = "API endpoint for get address detail")
    @GetMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressDetail(
            Authentication authentication,
            @PathVariable Long addressId) {
        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        log.info("Get address for user: {}", currentUser.getUsername());
        AddressResponse address = userService.getAddressById(currentUser.getUsername(), addressId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.get_address"), address));
    }

    @Operation(summary = "Initiate email change", description = "API endpoint for initiate email change")
    @PostMapping("/initiate-email-change")
    public ResponseEntity<ApiResponse<Void>> initiateEmailChange(
            Authentication authentication,
            @RequestBody @Valid InitiateEmailChangeRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Email change initiated for user: {}", user.getUsername());
        userService.initiateEmailChange(user.getUsername(), request.getNewEmail(), request.getCurrentPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                Translator.tolocale("success.user.otp_sent_to_new_email", request.getNewEmail()), null));
    }

    @Operation(summary = "Verify email change", description = "API endpoint for verify email change")
    @PostMapping("/verify-email-change")
    public ResponseEntity<ApiResponse<Void>> verifyEmailChange(
            Authentication authentication,
            @RequestBody @Valid VerifyOtpRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Email changed successfully for user: {}", user.getUsername());
        userService.verifyEmailChange(user.getUsername(), request.getOtp());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), Translator.tolocale("success.user.update_email"), null));
    }

    @Operation(summary = "Resend email verification", description = "API endpoint for resend email verification")
    @PostMapping("/resend-email-verification")
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Resent Email Change OTP for user {}", user.getUsername());
        userService.resendEmailChangeOtp(user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                Translator.tolocale("success.user.otp_resent_to_new_email"), null));
    }

    @Operation(summary = "Initiate phone change", description = "API endpoint for initiate phone change")
    @PostMapping("/initiate-phone-change")
    public ResponseEntity<ApiResponse<Void>> initiatePhoneChange(
            Authentication authentication,
            @RequestBody @Valid InitiatePhoneChangeRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Phone change initiated for user: {}", user.getUsername());
        userService.initiatePhoneChange(user.getUsername(), request.getNewPhone(), request.getCurrentPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                Translator.tolocale("success.user.otp_phone_sent"), null));
    }

    @Operation(summary = "Verify phone change", description = "API endpoint for verify phone change")
    @PostMapping("/verify-phone-change")
    public ResponseEntity<ApiResponse<Void>> verifyPhoneChange(
            Authentication authentication,
            @RequestBody @Valid VerifyOtpRequest request) {

        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("Phone changed successfully for user: {}", user.getUsername());
        userService.verifyPhoneChange(user.getUsername(), request.getOtp());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                Translator.tolocale("success.user.phone_updated"), null));
    }

    @Operation(summary = "Resend phone verification", description = "API endpoint for resend phone verification")
    @PostMapping("/resend-phone-change-verification")
    public ResponseEntity<ApiResponse<Void>> resendPhoneVerification(Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();

        userService.resendPhoneChangeOtp(user.getUsername());
        log.info("Resent Phone Change OTP for user: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                Translator.tolocale("success.user.otp_phone_resent"), null));
    }

}