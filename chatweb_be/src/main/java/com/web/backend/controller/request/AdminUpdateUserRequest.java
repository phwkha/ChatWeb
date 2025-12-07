package com.web.backend.controller.request;

import com.web.backend.common.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String firstName;
    private String lastName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(
            regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;
    private Role role;
}