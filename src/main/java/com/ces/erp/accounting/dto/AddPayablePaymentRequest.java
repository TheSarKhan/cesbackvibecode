package com.ces.erp.accounting.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddPayablePaymentRequest {
    private Long invoiceId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String note;
}
