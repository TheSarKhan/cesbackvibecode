package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class MonthlyTrendItem {
    private String month;       // "Yan 2026" or "2026-01"
    private Integer monthNum;   // 1-12
    private Integer year;
    private BigDecimal income;
    private BigDecimal contractorExpense;
    private BigDecimal companyExpense;
    private BigDecimal netProfit;
}
