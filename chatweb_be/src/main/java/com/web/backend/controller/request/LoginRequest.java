package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{valid.username_empty}")
    private String username;

    @NotBlank(message = "{valid.pwd_empty}")
    @Size(min = 6, message = "{valid.pwd_min_6}")
    private String password;

}
