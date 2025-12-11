package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RoleRequest {
    @NotBlank(message = "Tên role không được để trống")
    private String name;

    private String description;

    private List<Long> permissionIds;
}