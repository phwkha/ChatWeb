package com.web.backend.service;

import com.web.backend.controller.request.VerifyOtpRequest;

public interface OtpService {

    void verifyUser(VerifyOtpRequest request);

    void resendOtp(String email);

    void verifyEmailChange(String username, String otp);

    void verifyPasswordReset(String email, String otp, String newPassword);

    void verifyPhoneChange(String username, String otp);

    void resendForgotPasswordOtp(String email);

    void resendPhoneChangeOtp(String username);

    void resendEmailChangeOtp(String username);

}
