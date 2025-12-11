package com.web.backend.mapper;

import com.web.backend.controller.request.*;
import com.web.backend.controller.response.*;
import com.web.backend.model.AddressEntity;
import com.web.backend.model.PermissionEntity;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    @Mapping(target = "permissions", source = "entity", qualifiedByName = "mapPermissionsToStrings") // Thêm dòng này
    UserResponse toUserDTO(UserEntity entity);

    @Named("mapPermissionsToStrings")
    default Set<String> mapPermissionsToStrings(UserEntity entity) {
        if (entity == null) return null;
        return entity.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toSet());
    }

    @Named("mapRolesToStrings")
    default Set<String> mapRolesToStrings(Set<RoleEntity> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
    }

    AddressResponse toAddressDTO(AddressEntity entity);

    UserSummaryResponse toUserSummaryDTO(UserEntity entity);

    UserDetailResponse toUserDetailDTO(UserEntity entity);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    UserEntity toEntity(CreateUserRequest request);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    UserEntity toEntity(AdminCreateUserRequest request);

    AddressEntity toAddressEntity(AddressRequest request);

    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget UserEntity entity);

    void updateAdminUserFromRequest(AdminUpdateUserRequest request, @MappingTarget UserEntity entity);

    void updateAddressFromRequest(AddressRequest request, @MappingTarget AddressEntity entity);

    PermissionResponse toPermissionDTO(PermissionEntity entity);

    @Mapping(target = "permissions", source = "permissions")
    RoleResponse toRoleDTO(RoleEntity entity);

    void updateRoleFromRequest(RoleRequest request, @MappingTarget RoleEntity entity);
}