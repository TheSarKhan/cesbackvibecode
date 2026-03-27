package com.ces.erp.common.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // ── Dashboard panel data ──────────────────────────────────────────────────
    private List<ProjectDto>          projects;
    private List<RequestDto>          requests;
    private AccountingSummaryDto      accountingSummary;
    private List<InvoiceDto>          invoices;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ProjectDto {
        private Long   id;
        private String status;
        private String companyName;
        private String projectCode;
        private String endDate;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RequestDto {
        private Long   id;
        private String status;
        private String companyName;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AccountingSummaryDto {
        private BigDecimal totalIncome;
        private BigDecimal totalContractorExpense;
        private BigDecimal totalCompanyExpense;
        private BigDecimal netProfit;
        private long       incomeCount;
        private long       contractorExpenseCount;
        private long       companyExpenseCount;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class InvoiceDto {
        private String     issueDate;
        private BigDecimal amount;
    }
}
