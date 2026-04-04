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

    // İş saatı timesheet qaimələrindən izlənilir — optional
    private BigDecimal actualHours;

    // 1.0 = adi, 1.5 = iş vaxtından kənar — optional (default 1.0)
    private BigDecimal overtimeRate;
}
