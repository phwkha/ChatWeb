package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "{valid.current_pwd_empty}")
    private String currentPassword;

    @NotBlank(message = "{valid.new_pwd_empty}")
    @Size(min = 6, message = "{valid.new_pwd_min_6}")
    private String newPassword;
}