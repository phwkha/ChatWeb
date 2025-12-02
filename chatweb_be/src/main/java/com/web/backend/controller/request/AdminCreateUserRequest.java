package com.web.backend.controller.request;

import com.web.backend.common.Role; // Import Role enum của bạn
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String email;
    private Role role;
}