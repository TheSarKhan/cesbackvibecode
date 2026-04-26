package com.ces.erp.accounting.dto.report;

import com.ces.erp.enums.ReceivableStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ReceivableReportItem {
    private Long id;
    private String projectCode;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private ReceivableStatus status;
}
