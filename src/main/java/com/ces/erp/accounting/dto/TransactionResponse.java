package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String type;
    private String category;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String paymentMethod;
    private String referenceNumber;
    private String description;
    private Long projectId;
    private Long contractorId;
    private Long customerId;
    private String notes;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .category(t.getCategory())
                .amount(t.getAmount())
                .transactionDate(t.getTransactionDate())
                .paymentMethod(t.getPaymentMethod())
                .referenceNumber(t.getReferenceNumber())
                .description(t.getDescription())
                .projectId(t.getProjectId())
                .contractorId(t.getContractorId())
                .customerId(t.getCustomerId())
                .notes(t.getNotes())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
