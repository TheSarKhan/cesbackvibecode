package com.ces.erp.coordinator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CoordinatorPlanRequest {

    private Long operatorId;

    private Integer dayCount;
    private BigDecimal equipmentPrice;
    private BigDecimal contractorDailyRate;  // günlük dərəcə (frontend-dən gəlir)
    private BigDecimal contractorPayment;    // cəmi (dayCount * dailyRate — backend hesablayır, ya da manual)
    private BigDecimal operatorPayment;
    private BigDecimal transportationPrice;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<Long> safetyEquipmentIds;

    private String notes;
}
