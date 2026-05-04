package com.ces.erp.hr.dto;

import com.ces.erp.enums.AttendanceStatus;
import com.ces.erp.hr.entity.AttendanceRecord;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AttendanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private LocalDate date;
    private AttendanceStatus status;
    private BigDecimal hoursWorked;
    private String notes;

    public static AttendanceResponse from(AttendanceRecord a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .employeeId(a.getEmployee() != null ? a.getEmployee().getId() : null)
                .employeeFullName(a.getEmployee() != null ? a.getEmployee().getFullName() : null)
                .date(a.getDate())
                .status(a.getStatus())
                .hoursWorked(a.getHoursWorked())
                .notes(a.getNotes())
                .build();
    }
}
