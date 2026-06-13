package com.ces.erp.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionUpdateRequest {
    @NotBlank(message = "Etiket boş ola bilməz")
    private String labelAz;
    private String description;
}
