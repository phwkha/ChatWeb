package com.web.backend.controller;

import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody LoginRequest loginRequest) {

        System.out.println(loginRequest);

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
                .body(loginResponse.getUserDTO());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return authenticationService.logout();
    }
}
