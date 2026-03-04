package com.ces.erp.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleRequest {

    @NotBlank(message = "Rol adı boş ola bilməz")
    private String name;

    private String description;

    @NotNull(message = "Şöbə ID boş ola bilməz")
    private Long departmentId;

    private List<PermissionRequest> permissions;
}
