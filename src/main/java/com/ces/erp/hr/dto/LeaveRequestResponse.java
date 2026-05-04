package com.ces.erp.hr.dto;

import com.ces.erp.enums.LeaveStatus;
import com.ces.erp.enums.LeaveType;
import com.ces.erp.hr.entity.LeaveRequest;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveRequestResponse {

    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private String employeeCode;
    private LeaveType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private String reason;
    private LeaveStatus status;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private String decisionNote;
    private LocalDateTime createdAt;

    public static LeaveRequestResponse from(LeaveRequest l) {
        return LeaveRequestResponse.builder()
                .id(l.getId())
                .employeeId(l.getEmployee() != null ? l.getEmployee().getId() : null)
                .employeeFullName(l.getEmployee() != null ? l.getEmployee().getFullName() : null)
                .employeeCode(l.getEmployee() != null ? l.getEmployee().getEmployeeCode() : null)
                .type(l.getType())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .days(l.getDays())
                .reason(l.getReason())
                .status(l.getStatus())
                .decidedBy(l.getDecidedBy())
                .decidedAt(l.getDecidedAt())
                .decisionNote(l.getDecisionNote())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
