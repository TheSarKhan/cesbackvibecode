package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.ReceivablePayment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ReceivablePaymentResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private Long invoiceId;
    private String note;

    public static ReceivablePaymentResponse from(ReceivablePayment rp) {
        return ReceivablePaymentResponse.builder()
                .id(rp.getId())
                .amount(rp.getAmount())
                .paymentDate(rp.getPaymentDate())
                .invoiceId(rp.getInvoice() != null ? rp.getInvoice().getId() : null)
                .note(rp.getNote())
                .build();
    }
}
