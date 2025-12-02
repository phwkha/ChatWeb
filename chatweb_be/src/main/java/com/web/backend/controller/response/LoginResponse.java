package com.web.backend.controller.response;

import com.web.backend.model.DTO.UserDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private UserDTO userDTO;
}
