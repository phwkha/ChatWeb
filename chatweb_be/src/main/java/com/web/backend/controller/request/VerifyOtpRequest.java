package com.web.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "{valid.email_empty}")
    @Email(message = "{valid.email_format}")
    private String email;

    @NotBlank(message = "{valid.otp_empty}")
    private String otp;
}