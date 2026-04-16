package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.DocumentLine;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DocumentLineResponse {

    private Long id;
    private int lineOrder;
    private String description;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Long sourceInvoiceId;

    public static DocumentLineResponse from(DocumentLine line) {
        return DocumentLineResponse.builder()
                .id(line.getId())
                .lineOrder(line.getLineOrder())
                .description(line.getDescription())
                .unit(line.getUnit())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .totalPrice(line.getTotalPrice())
                .sourceInvoiceId(line.getSourceInvoiceId())
                .build();
    }
}
