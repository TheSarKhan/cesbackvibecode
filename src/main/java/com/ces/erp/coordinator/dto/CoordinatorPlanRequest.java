package com.ces.erp.coordinator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CoordinatorPlanRequest {

    private Long operatorId;

    private BigDecimal equipmentPrice;
    private BigDecimal contractorPayment;
    private BigDecimal transportationPrice;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean hasFlashingLights;
    private boolean hasFireExtinguisher;
    private boolean hasFirstAid;

    private String notes;
}
