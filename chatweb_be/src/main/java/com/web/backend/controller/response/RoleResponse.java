package com.web.backend.controller.response;

import com.web.backend.repository.PermissionRepository;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private Set<com.web.backend.controller.DTO.PermissionResponse> permissions;
}