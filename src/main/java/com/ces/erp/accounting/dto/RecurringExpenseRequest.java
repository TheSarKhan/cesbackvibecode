package com.ces.erp.accounting.dto;

import com.ces.erp.enums.RecurrenceFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecurringExpenseRequest {

    @NotBlank(message = "Ad tələb olunur")
    private String name;

    @NotBlank(message = "Kateqoriya tələb olunur")
    private String categoryKey;

    @NotBlank(message = "Kateqoriya adı tələb olunur")
    private String categoryLabel;

    @NotBlank(message = "Mənbə tələb olunur")
    private String sourceKey;

    @NotBlank(message = "Mənbə adı tələb olunur")
    private String sourceLabel;

    private BigDecimal amount;           // null/0 = dəyişkən məbləğ

    @NotNull(message = "Tezlik tələb olunur")
    private RecurrenceFrequency frequency;

    private Integer dayOfMonth;          // 1-31, ixtiyari

    private String notes;

    private boolean active = true;
}
