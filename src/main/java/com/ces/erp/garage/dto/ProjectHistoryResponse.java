package com.ces.erp.garage.dto;

import com.ces.erp.garage.entity.EquipmentProjectHistory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectHistoryResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal contractorRevenue;
    private String status;
    private String notes;
    private LocalDateTime createdAt;

    public static ProjectHistoryResponse from(EquipmentProjectHistory h) {
        return ProjectHistoryResponse.builder()
                .id(h.getId())
                .projectId(h.getProjectId())
                .projectName(h.getProjectName())
                .startDate(h.getStartDate())
                .endDate(h.getEndDate())
                .contractorRevenue(h.getContractorRevenue())
                .status(h.getStatus())
                .notes(h.getNotes())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
