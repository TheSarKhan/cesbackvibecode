package com.ces.erp.common.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalCustomers;
    private long totalContractors;
    private long totalInvestors;
    private long totalOperators;
    private long totalEmployees;

    private long availableEquipment;
    private long rentedEquipment;
    private long defectiveEquipment;
    private long outOfServiceEquipment;

    private long pendingApprovals;
    private long activeRequests;
    private long activeProjects;
    private long deletedRecords;
}
