package com.ces.erp.project.dto;

import com.ces.erp.project.entity.ProjectDowntime;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDowntimeResponse {
    private Long id;
    private Long projectId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reasonType;
    private String reasonDescription;
    private boolean isPaid;
    private BigDecimal standbyRate;
    private boolean autoExtendEndDate;
    private String resolvedNotes;
    private String status;
    private LocalDateTime createdAt;

    public static ProjectDowntimeResponse from(ProjectDowntime d) {
        if (d == null) return null;
        return ProjectDowntimeResponse.builder()
                .id(d.getId())
                .projectId(d.getProject().getId())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .reasonType(d.getReasonType())
                .reasonDescription(d.getReasonDescription())
                .isPaid(d.isPaid())
                .standbyRate(d.getStandbyRate())
                .autoExtendEndDate(d.isAutoExtendEndDate())
                .resolvedNotes(d.getResolvedNotes())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
