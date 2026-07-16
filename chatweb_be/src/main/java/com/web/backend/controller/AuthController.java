package com.web.backend.controller;

import com.web.backend.controller.request.*;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.controller.response.TokenResponse;
import com.web.backend.controller.response.UserResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.AuthenticationService;
import com.web.backend.service.util.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import com.web.backend.common.TokenType;
import com.web.backend.config.LocalResolverConfig.Translator;

@Tag(name = "Auth Controller")
@RestController
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
@Slf4j(topic = "AUTH-CONTROLLER")
public class AuthController {

        private final AuthenticationService authenticationService;

        private final RateLimitingService rateLimitingService;

        @Operation(summary = "Login", description = "API endpoint for login")
        @PostMapping("/login")
        public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody @Valid LoginRequest loginRequest,
                        HttpServletRequest request) {

                String ip = request.getRemoteAddr();

                if (!rateLimitingService.allowRequest(ip, "login", 5, 60)) {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                        .body(ApiResponse.error(429,
                                                        Translator.tolocale("error.auth.too_many_attempts")));
                }

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
                                .path("/api/auth/refresh-token")
                                .maxAge(7 * 24 * 60 * 60)
                                .sameSite("strict")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(ApiResponse.success(HttpStatus.OK.value(),
                                                Translator.tolocale("success.auth.login"),
                                                loginResponse.getUserResponse()));
        }

        @Operation(summary = "Register user", description = "API endpoint for register user")
        @PostMapping("/register")
        public ResponseEntity<ApiResponse<UserResponse>> registerUser(
                        @RequestBody @Valid CreateUserRequest createUserRequest) {
                log.info("Registering new user: {}", createUserRequest.getUsername());

                UserResponse newUser = authenticationService.createUser(createUserRequest);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(HttpStatus.CREATED.value(),
                                                Translator.tolocale("success.auth.registered"), newUser));
        }

        @Operation(summary = "Verify otp", description = "API endpoint for verify otp")
        @PostMapping("/verify-account")
        public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
                log.info("Verify Otp Request: {}", request);
                authenticationService.verifyUser(request);
                return ResponseEntity
                                .ok(ApiResponse.success(HttpStatus.OK.value(),
                                                Translator.tolocale("success.auth.activated"), null));
        }

        @Operation(summary = "Resend otp", description = "API endpoint for resend otp")
        @PostMapping("/resend-otp")
        public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String email) {
                log.info("Resend Otp Request: {}", email);
                authenticationService.resendOtp(email);
                return ResponseEntity
                                .ok(ApiResponse.success(HttpStatus.OK.value(),
                                                Translator.tolocale("success.auth.otp_resent"), null));
        }

        @Operation(summary = "Refresh token", description = "API endpoint for refresh token")
        @PostMapping("/refresh-token")
        public ResponseEntity<ApiResponse<String>> refreshToken(
                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {

                log.info("Refresh token with user");
                TokenResponse newTokenResponse = authenticationService.refreshToken(refreshToken);

                ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", newTokenResponse.getAccessToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(15 * 60)
                                .sameSite("Strict")
                                .build();

                ResponseCookie newrefreshCookie = ResponseCookie
                                .from("refreshToken", newTokenResponse.getRefreshToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/api/auth")
                                .maxAge(7 * 24 * 60 * 60)
                                .sameSite("strict")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, newrefreshCookie.toString())
                                .body(ApiResponse.success(HttpStatus.OK.value(),
                                                Translator.tolocale("success.auth.token_refreshed"),
                                                newTokenResponse.getAccessToken()));
        }

        @Operation(summary = "Forgot password", description = "API endpoint for forgot password")
        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
                log.info("Password reset initiated for email");
                authenticationService.initiateForgotPassword(request.getEmail());
                return ResponseEntity
                                .ok(ApiResponse.success(HttpStatus.OK.value(),
                                                Translator.tolocale("success.auth.code_sent"), null));
        }

        @Operation(summary = "Reset password", description = "API endpoint for reset password")
        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
                log.info("Password reset successfully for user");
                authenticationService.verifyPasswordReset(request.getEmail(), request.getOtp(),
                                request.getNewPassword());
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                                Translator.tolocale("success.auth.pwd_reset"), null));
        }

        @Operation(summary = "Resend forgot password", description = "API endpoint for resend forgot password")
        @PostMapping("/resend-forgot-password")
        public ResponseEntity<ApiResponse<Void>> resendForgotPassword(@RequestParam String email) {
                authenticationService.resendForgotPasswordOtp(email);
                return ResponseEntity
                                .ok(ApiResponse.success(HttpStatus.OK.value(),
                                                Translator.tolocale("success.auth.code_resent"), null));
        }

        @Operation(summary = "Logout", description = "API endpoint for logout")
        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<String>> logout(Authentication authentication, HttpServletRequest request) {
                UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
                log.info("User logout {}", userEntityPrincipal.getUsername());
                String accesstoken = null;
                String refreshtoken = null;
                if (request.getCookies() != null) {
                        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                                if ("accessToken".equals(cookie.getName())) {
                                        accesstoken = cookie.getValue();
                                        break;
                                }
                        }
                }
                if (request.getCookies() != null) {
                        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                                if ("refreshToken".equals(cookie.getName())) {
                                        refreshtoken = cookie.getValue();
                                        break;
                                }
                        }
                }
                if (accesstoken == null || accesstoken.isEmpty()) {
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                accesstoken = authHeader.substring(7);
                        }
                }

                if (accesstoken != null && !accesstoken.trim().isEmpty() && !accesstoken.equals("null")
                                && !accesstoken.equals("undefined")) {
                        authenticationService.logout(accesstoken, TokenType.ACCESS_TOKEN);
                }
                if (refreshtoken != null && !refreshtoken.trim().isEmpty() && !refreshtoken.equals("null")
                                && !refreshtoken.equals("undefined")) {
                        authenticationService.logout(refreshtoken, TokenType.REFRESH_TOKEN);
                }

                ResponseCookie deleteAccess = ResponseCookie.from("accessToken", "")
                                .path("/").maxAge(0).build();

                ResponseCookie deleteRefresh = ResponseCookie.from("refreshToken", "")
                                .path("/api/auth").maxAge(0).build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteAccess.toString())
                                .header(HttpHeaders.SET_COOKIE, deleteRefresh.toString())
                                .body(ApiResponse.success(200, Translator.tolocale("success.auth.logout"), null));
        }

        @Operation(summary = "Logout all devices", description = "API endpoint to invalidate all refresh tokens globally")
        @PostMapping("/logout-all-devices")
        public ResponseEntity<ApiResponse<String>> logoutAllDevices(Authentication authentication,
                        HttpServletRequest request) {
                UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
                log.info("User logout all devices {}", userEntityPrincipal.getUsername());

                authenticationService.logoutAllDevices(userEntityPrincipal.getUsername());

                String accesstoken = null;
                String refreshtoken = null;
                if (request.getCookies() != null) {
                        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                                if ("accessToken".equals(cookie.getName())) {
                                        accesstoken = cookie.getValue();
                                }
                                if ("refreshToken".equals(cookie.getName())) {
                                        refreshtoken = cookie.getValue();
                                }
                        }
                }
                if (accesstoken == null || accesstoken.isEmpty()) {
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                accesstoken = authHeader.substring(7);
                        }
                }

                if (accesstoken != null && !accesstoken.trim().isEmpty() && !accesstoken.equals("null")
                                && !accesstoken.equals("undefined")) {
                        authenticationService.logout(accesstoken, TokenType.ACCESS_TOKEN);
                }
                if (refreshtoken != null && !refreshtoken.trim().isEmpty() && !refreshtoken.equals("null")
                                && !refreshtoken.equals("undefined")) {
                        authenticationService.logout(refreshtoken, TokenType.REFRESH_TOKEN);
                }

                ResponseCookie deleteAccess = ResponseCookie.from("accessToken", "")
                                .path("/").maxAge(0).build();

                ResponseCookie deleteRefresh = ResponseCookie.from("refreshToken", "")
                                .path("/api/auth").maxAge(0).build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteAccess.toString())
                                .header(HttpHeaders.SET_COOKIE, deleteRefresh.toString())
                                .body(ApiResponse.success(200, Translator.tolocale("success.auth.logout"), null));
        }
}
