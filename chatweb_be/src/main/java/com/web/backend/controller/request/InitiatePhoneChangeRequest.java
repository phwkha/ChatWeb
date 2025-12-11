package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InitiatePhoneChangeRequest {
    @NotBlank
    @Pattern(regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$", message = "Số điện thoại không hợp lệ")
    private String newPhone;

    @NotBlank(message = "Cần nhập mật khẩu để xác nhận")
    private String currentPassword;
}