package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ReportSummaryResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalContractorExpense;
    private BigDecimal totalCompanyExpense;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    
    // Percentage changes vs previous period (if requested)
    private Double incomeChangeScore; 
    private Double expenseChangeScore;
    private Double profitChangeScore;
    
    private Long incomeCount;
    private Long contractorExpenseCount;
    private Long companyExpenseCount;
    private BigDecimal avgInvoiceAmount;
}
