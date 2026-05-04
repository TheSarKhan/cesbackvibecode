package com.ces.erp.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayrollPeriodRequest {

    @NotNull
    @Min(2000)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    private Integer workingDaysInMonth;
    private Integer workingHoursPerDay;
    private String notes;
}
