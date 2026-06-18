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
    private TrendsDto                 trends;
    private ArAgingDto                arAging;
    private List<TopCustomerDto>      topCustomers;

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

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class TrendsDto {
        private BigDecimal incomeTrend;   // % dəyişim — bu ay vs keçən ay (gəlir)
        private BigDecimal profitTrend;   // % dəyişim — bu ay vs keçən ay (xalis mənfəət)
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ArAgingDto {
        private BigDecimal totalOutstanding;
        private BigDecimal current;        // ≤30 gün / vaxtı çatmamış
        private BigDecimal days30to60;
        private BigDecimal days60to90;
        private BigDecimal overdue90Plus;
        private long       overdueInvoiceCount;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class TopCustomerDto {
        private Long       id;
        private String     name;
        private long       invoiceCount;
        private BigDecimal totalRevenue;
    }
}
