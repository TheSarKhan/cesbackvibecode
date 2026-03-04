package com.ces.erp.garage.dto;

import com.ces.erp.garage.entity.EquipmentInspection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InspectionResponse {

    private Long id;
    private LocalDate inspectionDate;
    private String description;
    private Long performedByUserId;
    private String performedByUserName;
    private String documentName;
    private String documentPath;
    private LocalDateTime createdAt;

    public static InspectionResponse from(EquipmentInspection i) {
        return InspectionResponse.builder()
                .id(i.getId())
                .inspectionDate(i.getInspectionDate())
                .description(i.getDescription())
                .performedByUserId(i.getPerformedBy() != null ? i.getPerformedBy().getId() : null)
                .performedByUserName(i.getPerformedBy() != null ? i.getPerformedBy().getFullName() : null)
                .documentName(i.getDocumentName())
                .documentPath(i.getDocumentPath())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
