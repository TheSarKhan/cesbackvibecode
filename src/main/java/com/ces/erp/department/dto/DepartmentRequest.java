package com.ces.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Şöbə adı boş ola bilməz")
    private String name;

    private String description;
}
