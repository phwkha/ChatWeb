package com.web.backend.common;


import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;

public class Convert {

    public  static UserDTO UserConvertToUserDTO(UserEntity userEntity) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userEntity.getId());
        userDTO.setUsername(userEntity.getUsername());
        userDTO.setEmail(userEntity.getEmail());
        userDTO.setOnline(userEntity.isOnline());
        userDTO.setUserStatus(userEntity.getUserStatus());
        userDTO.setPhone(userEntity.getPhone());
        userDTO.setFullName(userEntity.getFullName());
        userDTO.setRole(userEntity.getRole());

        return userDTO;
    }
}
