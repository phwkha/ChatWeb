package com.web.backend.mapper;

import com.web.backend.controller.request.*;
import com.web.backend.model.AddressEntity;
import com.web.backend.model.DTO.AddressDTO;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserDTO toUserDTO(UserEntity entity);

    AddressDTO toAddressDTO(AddressEntity entity);

    @Mapping(target = "userStatus", constant = "ACTIVE")
    @Mapping(target = "role", constant = "USER")
    UserEntity toEntity(CreateUserRequest request);

    @Mapping(target = "userStatus", constant = "ACTIVE")
    UserEntity toEntity(AdminCreateUserRequest request);

    AddressEntity toAddressEntity(AddressRequest request);

    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget UserEntity entity);

    void updateAdminUserFromRequest(AdminUpdateUserRequest request, @MappingTarget UserEntity entity);

    void updateAddressFromRequest(AddressRequest request, @MappingTarget AddressEntity entity);
}