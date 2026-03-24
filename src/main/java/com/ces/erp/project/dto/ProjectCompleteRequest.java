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

    @NotNull(message = "Faktiki iş saatı daxil edilməlidir")
    @DecimalMin(value = "0.01", message = "Faktiki iş saatı 0-dan böyük olmalıdır")
    private BigDecimal actualHours;

    // 1.0 = adi, 1.5 = iş vaxtından kənar (fasilə ilə)
    @NotNull(message = "Əlavə vaxt dərəcəsi seçilməlidir")
    private BigDecimal overtimeRate;
}
