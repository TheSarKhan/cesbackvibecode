package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.GeneratedDocument;
import com.ces.erp.enums.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GeneratedDocumentResponse {

    private Long id;
    private String documentNumber;
    private DocumentType documentType;
    private LocalDate documentDate;

    private Long customerId;
    private String customerName;
    private String customerVoen;
    private String customerAddress;

    private LocalDate contractDate;
    private String contractNumber;

    private BigDecimal subtotal;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal grandTotal;

    private String pdfFilePath;
    private String sourceInvoiceIds;
    private String notes;

    private List<DocumentLineResponse> lines;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GeneratedDocumentResponse from(GeneratedDocument doc) {
        return GeneratedDocumentResponse.builder()
                .id(doc.getId())
                .documentNumber(doc.getDocumentNumber())
                .documentType(doc.getDocumentType())
                .documentDate(doc.getDocumentDate())
                .customerId(doc.getCustomer() != null ? doc.getCustomer().getId() : null)
                .customerName(doc.getCustomerName())
                .customerVoen(doc.getCustomerVoen())
                .customerAddress(doc.getCustomerAddress())
                .contractDate(doc.getContractDate())
                .contractNumber(doc.getContractNumber())
                .subtotal(doc.getSubtotal())
                .vatRate(doc.getVatRate())
                .vatAmount(doc.getVatAmount())
                .grandTotal(doc.getGrandTotal())
                .pdfFilePath(doc.getPdfFilePath())
                .sourceInvoiceIds(doc.getSourceInvoiceIds())
                .notes(doc.getNotes())
                .lines(doc.getLines().stream()
                        .sorted(java.util.Comparator.comparingInt(l -> l.getLineOrder()))
                        .map(DocumentLineResponse::from)
                        .toList())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
