package com.ces.erp.project.dto;

import com.ces.erp.project.entity.ProjectEquipmentSwap;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectEquipmentSwapResponse {
    private Long id;
    private Long projectId;
    private Long oldEquipmentId;
    private String oldEquipmentName;
    private String oldEquipmentPlateNumber;
    private Double oldEquipmentFinalCounter;
    private String oldEquipmentNextStatus;
    private Long newEquipmentId;
    private String newEquipmentName;
    private String newEquipmentPlateNumber;
    private Double newEquipmentInitialCounter;
    private LocalDate swapDate;
    private String swapReason;
    private String notes;
    private LocalDateTime createdAt;

    public static ProjectEquipmentSwapResponse from(ProjectEquipmentSwap s) {
        if (s == null) return null;
        return ProjectEquipmentSwapResponse.builder()
                .id(s.getId())
                .projectId(s.getProject().getId())
                .oldEquipmentId(s.getOldEquipment().getId())
                .oldEquipmentName(s.getOldEquipment().getName())
                .oldEquipmentPlateNumber(s.getOldEquipment().getPlateNumber())
                .oldEquipmentFinalCounter(s.getOldEquipmentFinalCounter())
                .oldEquipmentNextStatus(s.getOldEquipmentNextStatus())
                .newEquipmentId(s.getNewEquipment().getId())
                .newEquipmentName(s.getNewEquipment().getName())
                .newEquipmentPlateNumber(s.getNewEquipment().getPlateNumber())
                .newEquipmentInitialCounter(s.getNewEquipmentInitialCounter())
                .swapDate(s.getSwapDate())
                .swapReason(s.getSwapReason())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
