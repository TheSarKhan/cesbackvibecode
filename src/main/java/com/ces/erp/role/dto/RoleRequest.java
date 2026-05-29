package com.ces.erp.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RoleRequest {

    @NotBlank(message = "Rol adı boş ola bilməz")
    @Size(min = 2, max = 100, message = "Rol adı 2-100 simvol arasında olmalıdır")
    @Pattern(regexp = ".*\\p{L}.*", message = "Rol adı ən azı bir hərf içərməlidir")
    private String name;

    private String description;

    @NotNull(message = "Şöbə ID boş ola bilməz")
    private Long departmentId;

    // Dinamik icazə kataloqundan verilən icazə ID-ləri
    private List<Long> permissionIds;

    private List<Long> approvalDepartmentIds;
}
