package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PartnerReportItem {
    private String type; // "CONTRACTOR" or "INVESTOR"
    private Long id;
    private String name;
    private String voen;
    private BigDecimal totalExpense;
    private Long invoiceCount;
    private LocalDate lastPaymentDate;
}
