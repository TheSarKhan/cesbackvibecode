package com.ces.erp.accounting.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddPaymentRequest {
    private BigDecimal amount;
    private LocalDate paymentDate;
    private Long invoiceId;
    private String note;
}
