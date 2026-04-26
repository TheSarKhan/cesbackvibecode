package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ExpenseBreakdownItem {
    private String category; // "CONTRACTOR_EXPENSE", "COMPANY_EXPENSE"
    private String categoryLabel; 
    private BigDecimal amount;
    private Double percentage;
    private Long count;
}
