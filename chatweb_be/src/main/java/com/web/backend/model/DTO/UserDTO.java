package com.web.backend.model.DTO;

import com.web.backend.common.Role;
import com.web.backend.common.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private boolean isOnline;
    private UserStatus userStatus;
    private String fullName;
    private String phone;
    private Role role;

}
