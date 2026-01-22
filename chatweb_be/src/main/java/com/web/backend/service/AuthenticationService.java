package com.web.backend.service;

import com.web.backend.controller.request.CreateUserRequest;
import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.request.VerifyOtpRequest;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.controller.response.UserResponse;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {

    void logout(String accessToken);

    LoginResponse login(LoginRequest loginRequest);

    String refreshToken(String refreshToken);

    void initiateForgotPassword(String email);

    UserResponse createUser(CreateUserRequest createUserRequest);

    void verifyUser(VerifyOtpRequest request);

    void resendOtp(String email);

    void verifyPasswordReset(String email, String otp, String newPassword);

    void resendForgotPasswordOtp(String email);
}
