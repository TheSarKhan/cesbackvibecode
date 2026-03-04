package com.ces.erp.garage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionRequest {

    @NotNull(message = "Baxış tarixi boş ola bilməz")
    private LocalDate inspectionDate;

    private String description;
    private Long performedByUserId;
}
