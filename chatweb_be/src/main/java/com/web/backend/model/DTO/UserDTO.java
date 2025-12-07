package com.web.backend.model.DTO;

import com.web.backend.common.GenderType;
import com.web.backend.common.Role;
import com.web.backend.common.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private boolean isOnline;

    private UserStatus userStatus;

    private Role role;

    private String firstName;

    private String lastName;

    private String avatar;

    private Date birthday;

    private GenderType gender;

    private Date createAt;

    private Date updateAt;

    private List<AddressDTO> addresses;

}
