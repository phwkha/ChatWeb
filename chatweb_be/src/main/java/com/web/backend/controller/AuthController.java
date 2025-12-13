package com.web.backend.controller;

import com.web.backend.controller.request.ForgotPasswordRequest;
import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.request.ResetPasswordRequest;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.controller.response.UserResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.AuthenticationService;
import com.web.backend.service.OtpService;
import com.web.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j(topic = "AUTH-CONTROLLER")
public class AuthController {

    private final AuthenticationService authenticationService;

    private final UserService userService;

    private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody @Valid LoginRequest loginRequest) {
        
        log.info("Login with user: {}", loginRequest.getUsername());

        LoginResponse loginResponse = authenticationService.login(loginRequest);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("strict")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh-token")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "Đăng nhập thành công",null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication, HttpServletRequest request) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("User logout {}", userEntityPrincipal.getUsername());
        String token = request.getHeader("Authorization").substring(7);
        authenticationService.logout(token);

        ResponseCookie deleteAccess = ResponseCookie.from("accessToken", "")
                .path("/").maxAge(0).build();

        ResponseCookie deleteRefresh = ResponseCookie.from("refreshToken", "")
                .path("/auth/refresh-token").maxAge(0).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteAccess.toString())
                .header(HttpHeaders.SET_COOKIE, deleteRefresh.toString())
                .body(ApiResponse.success(200, "Đăng xuất thành công", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        log.info("Refresh token with user");
        String newAccessToken = authenticationService.refreshToken(refreshToken);

        ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "Làm mới Token thành công", newAccessToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        log.info("Password reset initiated for email");
        userService.initiateForgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Mã xác nhận đã được gửi đến email của bạn.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        log.info("Password reset successfully for user");
        otpService.verifyPasswordReset(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.", null));
    }

    @PostMapping("/resend-forgot-password")
    public ResponseEntity<ApiResponse<Void>> resendForgotPassword(@RequestParam String email) {
        otpService.resendForgotPasswordOtp(email);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Đã gửi lại mã xác nhận vào email.", null));
    }
}
