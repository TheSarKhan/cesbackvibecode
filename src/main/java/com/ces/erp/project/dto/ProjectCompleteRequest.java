package com.ces.erp.project.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectCompleteRequest {

    @NotNull(message = "Evakuator xərci daxil edilməlidir")
    @DecimalMin(value = "0.0", message = "Evakuator xərci mənfi ola bilməz")
    private BigDecimal evacuationCost;

    @NotNull(message = "Planlaşdırılan iş saatı daxil edilməlidir")
    @DecimalMin(value = "0.01", message = "Planlaşdırılan iş saatı 0-dan böyük olmalıdır")
    private BigDecimal scheduledHours;

    @NotNull(message = "Faktiki iş saatı daxil edilməlidir")
    @DecimalMin(value = "0.01", message = "Faktiki iş saatı 0-dan böyük olmalıdır")
    private BigDecimal actualHours;
}
