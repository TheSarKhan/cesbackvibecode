package com.ces.erp.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectResumeRequest {
    @NotNull(message = "Bərpa tarixi tələb olunur")
    private LocalDate resumeDate;

    private String resolvedNotes;

    private boolean autoExtendEndDate = true;
}
