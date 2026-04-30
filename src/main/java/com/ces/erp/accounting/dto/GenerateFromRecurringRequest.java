package com.ces.erp.accounting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GenerateFromRecurringRequest {

    @NotNull(message = "Tarix tələb olunur")
    private LocalDate invoiceDate;

    private BigDecimal amountOverride;   // null = şablondakı məbləğ istifadə olunur
}
