package com.ces.erp.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectPaymentEntryRequest {

    @NotNull(message = "Məbləğ mütləqdir")
    @Positive(message = "Məbləğ müsbət olmalıdır")
    private BigDecimal amount;

    @NotNull(message = "Tarix mütləqdir")
    private LocalDate paymentDate;

    private String note;
}
