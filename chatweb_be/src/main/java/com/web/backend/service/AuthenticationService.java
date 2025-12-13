package com.web.backend.service;

import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.response.LoginResponse;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {
    void logout(String accessToken);
    LoginResponse login(LoginRequest loginRequest);
    String refreshToken(String refreshToken);
}
