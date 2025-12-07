package com.web.backend.controller;

import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDTO>> login(@RequestBody @Valid LoginRequest loginRequest) {

        LoginResponse loginResponse = authenticationService.login(loginRequest);

        ResponseCookie responseCookie = ResponseCookie.from("JWT", loginResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(1*60*60)
                .sameSite("strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "Đăng nhập thành công", loginResponse.getUserDTO()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        authenticationService.logout();

        ResponseCookie cookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "Đăng xuất thành công", null));
    }
}
