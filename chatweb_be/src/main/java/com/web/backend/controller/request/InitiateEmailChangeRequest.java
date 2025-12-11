package com.web.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitiateEmailChangeRequest {
    @NotBlank(message = "Email mới không được để trống")
    @Email(message = "Email không hợp lệ")
    private String newEmail;

    @NotBlank(message = "Cần nhập mật khẩu hiện tại để xác nhận")
    private String currentPassword;
}