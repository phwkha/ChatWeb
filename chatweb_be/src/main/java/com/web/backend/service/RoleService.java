package com.web.backend.service;

import com.web.backend.controller.request.RoleRequest;
import com.web.backend.controller.response.PermissionResponse;
import com.web.backend.controller.response.RoleResponse;

import java.util.List;

import org.springframework.lang.NonNull;

public interface RoleService {
    List<RoleResponse> getAllRoles();

    List<PermissionResponse> getAllPermissions();

    RoleResponse createRole(RoleRequest request);

    RoleResponse updateRole(@NonNull Long roleId, RoleRequest request);

    void deleteRole(@NonNull Long roleId);
}