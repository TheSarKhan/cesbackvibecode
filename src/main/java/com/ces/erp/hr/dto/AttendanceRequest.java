package com.ces.erp.hr.dto;

import com.ces.erp.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AttendanceRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate date;

    @NotNull
    private AttendanceStatus status;

    private BigDecimal hoursWorked;
    private String notes;
}
