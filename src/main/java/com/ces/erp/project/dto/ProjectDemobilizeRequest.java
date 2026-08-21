package com.ces.erp.project.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectDemobilizeRequest {
    private BigDecimal finalHourKmCounter;
    private String returnNotes;
    private boolean requiresInspection = false;
}
