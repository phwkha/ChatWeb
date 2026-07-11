package com.web.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "{valid.email_empty}")
    @Email(message = "{valid.email_invalid}")
    private String email;
}