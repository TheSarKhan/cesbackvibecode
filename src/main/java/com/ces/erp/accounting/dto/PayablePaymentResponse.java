package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.PayablePayment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayablePaymentResponse {
    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String note;

    public static PayablePaymentResponse from(PayablePayment pp) {
        return PayablePaymentResponse.builder()
                .id(pp.getId())
                .invoiceId(pp.getInvoice() != null ? pp.getInvoice().getId() : null)
                .invoiceNumber(pp.getInvoice() != null ? pp.getInvoice().getInvoiceNumber() : null)
                .amount(pp.getAmount())
                .paymentDate(pp.getPaymentDate())
                .note(pp.getNote())
                .build();
    }
}
