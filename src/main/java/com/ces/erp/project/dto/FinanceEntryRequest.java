package com.ces.erp.project.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceEntryRequest {

    @NotBlank(message = "Növ daxil edilməlidir")
    private String key;

    @NotNull(message = "Məbləğ daxil edilməlidir")
    @DecimalMin(value = "0.0", inclusive = false, message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal value;
}
