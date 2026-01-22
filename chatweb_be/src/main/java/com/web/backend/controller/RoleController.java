package com.web.backend.controller;

import com.web.backend.controller.request.RoleRequest;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.controller.response.PermissionResponse;
import com.web.backend.controller.response.RoleResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j(topic = "ROLE-CONTROLLER")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get all roles: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách Role thành công",
                roleService.getAllRoles()
        ));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW_ALL_PERMISSION')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions(Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get all permission: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách Permission thành công",
                roleService.getAllPermissions()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADD')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody @Valid RoleRequest request, Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Create role: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Tạo Role thành công",
                roleService.createRole(request)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(@PathVariable Long id, @RequestBody @Valid RoleRequest request, Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Update role: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Cập nhật Role thành công",
                roleService.updateRole(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id, Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Delete role: {}", userEntityPrincipal.getUsername());
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.NO_CONTENT.value(),
                "Xóa Role thành công",
                null
        ));
    }
}