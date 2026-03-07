package com.ces.erp.accounting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountingSummaryResponse {
    private BigDecimal totalIncome;             // A qaimələrinin cəmi
    private BigDecimal totalContractorExpense;  // B1 qaimələrinin cəmi
    private BigDecimal totalCompanyExpense;     // B2 qaimələrinin cəmi
    private BigDecimal totalExpense;            // B1 + B2
    private BigDecimal netProfit;               // A − (B1 + B2)
    private long incomeCount;
    private long contractorExpenseCount;
    private long companyExpenseCount;
}
