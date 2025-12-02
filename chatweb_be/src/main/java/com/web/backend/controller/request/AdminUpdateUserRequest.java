package com.web.backend.controller.request;

import com.web.backend.common.Role;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String fullName;
    private String email;
    private String phone;
    private Role role;
}
