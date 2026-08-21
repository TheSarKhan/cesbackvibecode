package com.ces.erp.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectEarlyTerminateRequest {
    @NotNull(message = "Xitam tarixi tələb olunur")
    private LocalDate terminationDate;

    private String terminationReason;

    private Double finalHourKmCounter;

    private boolean requiresInspection;

    private String returnNotes;
}
