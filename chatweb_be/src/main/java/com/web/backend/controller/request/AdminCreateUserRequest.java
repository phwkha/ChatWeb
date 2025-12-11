package com.web.backend.controller.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Họ không được để trống")
    private String firstName;
    private String lastName;

    @Pattern(
            regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private Long roleId;
}