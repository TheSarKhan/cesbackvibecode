package com.ces.erp.hr.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PositionRequest {
    @NotBlank(message = "Vəzifə adı boş ola bilməz")
    @Size(min = 2, max = 100, message = "Vəzifə adı 2-100 simvol arasında olmalıdır")
    private String name;

    @Size(max = 500, message = "Təsvir maksimum 500 simvol ola bilər")
    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Əməkhaqqı mənfi ola bilməz")
    @Digits(integer = 10, fraction = 2, message = "Əməkhaqqı formatı düzgün deyil")
    private BigDecimal defaultSalary;

    @NotNull(message = "Şöbə seçilməlidir")
    private Long departmentId;

    private Boolean active;
}
