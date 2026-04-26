package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProjectReportItem {
    private Long projectId;
    private String projectCode;
    private String companyName;
    private String projectName;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private Double profitMarginPercent;
    private Long invoiceCount;
}
