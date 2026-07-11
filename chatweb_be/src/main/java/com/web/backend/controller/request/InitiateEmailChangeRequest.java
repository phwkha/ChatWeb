package com.web.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitiateEmailChangeRequest {
    @NotBlank(message = "{valid.new_email_empty}")
    @Email(message = "{valid.email_invalid}")
    private String newEmail;

    @NotBlank(message = "{valid.require_current_pwd}")
    private String currentPassword;
}