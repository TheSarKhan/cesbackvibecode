package com.ces.erp.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectPauseRequest {
    @NotNull(message = "Dayanma tarixi tələb olunur")
    private LocalDate startDate;

    @NotBlank(message = "Dayanma səbəb tipi tələb olunur")
    private String reasonType; // WEATHER, CUSTOMER_SITE, TECHNICAL_BREAKDOWN, PAYMENT_DELAY, OTHER

    private String reasonDescription;

    private boolean isPaid;

    private BigDecimal standbyRate;

    private boolean autoExtendEndDate = true;
}
