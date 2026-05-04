package com.ces.erp.hr.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HrDashboardResponse {
    private long totalEmployees;
    private long activeEmployees;
    private long onLeaveEmployees;
    private long pendingLeaveRequests;

    private BigDecimal currentMonthGross;
    private BigDecimal currentMonthNet;
    private BigDecimal currentMonthCompanyCost;
    private Integer currentMonthEntryCount;
    private String currentMonthLabel;
    private String currentMonthStatus;
    private Long currentMonthPeriodId;
}
