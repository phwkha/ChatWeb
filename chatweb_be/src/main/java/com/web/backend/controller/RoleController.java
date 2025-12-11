package com.web.backend.controller;

import com.web.backend.controller.request.RoleRequest;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.PermissionResponse;
import com.web.backend.controller.response.RoleResponse;
import com.web.backend.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được quản lý Roles
public class RoleController {

    private final RoleService roleService;

    // 1. Lấy danh sách tất cả các Role
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách Role thành công",
                roleService.getAllRoles()
        ));
    }

    // 2. Lấy danh sách tất cả Permissions (để hiển thị checkbox trên Frontend)
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách Permission thành công",
                roleService.getAllPermissions()
        ));
    }

    // 3. Tạo Role mới
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody @Valid RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Tạo Role thành công",
                roleService.createRole(request)
        ));
    }

    // 4. Cập nhật Role (đổi tên, gán lại quyền)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(@PathVariable Long id, @RequestBody @Valid RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Cập nhật Role thành công",
                roleService.updateRole(id, request)
        ));
    }

    // 5. Xóa Role
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.NO_CONTENT.value(),
                "Xóa Role thành công",
                null
        ));
    }
}