package com.ces.erp.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PositionRequest {
    @NotBlank(message = "Vəzifə adı boş ola bilməz")
    private String name;
    private String description;
    private BigDecimal defaultSalary;
    private Long departmentId;
    private Boolean active;
}
