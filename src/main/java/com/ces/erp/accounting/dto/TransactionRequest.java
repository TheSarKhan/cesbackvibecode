package com.ces.erp.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotBlank(message = "Əməliyyat növü seçilməlidir")
    private String type;           // INCOME | EXPENSE

    @NotBlank(message = "Kateqoriya seçilməlidir")
    private String category;

    @NotNull(message = "Məbləğ daxil edilməlidir")
    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal amount;

    @NotNull(message = "Tarix seçilməlidir")
    private LocalDate transactionDate;

    private String paymentMethod;
    private String referenceNumber;
    private String description;
    private Long projectId;
    private Long contractorId;
    private Long customerId;
    private String notes;
}
