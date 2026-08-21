package com.ces.erp.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectEquipmentSwapRequest {
    @NotNull(message = "Cari (çıxarılan) texnika ID-si tələb olunur")
    private Long oldEquipmentId;

    private Double oldEquipmentFinalCounter;

    private String oldEquipmentNextStatus = "IN_REPAIR"; // IN_REPAIR, IN_INSPECTION, DEFECTIVE, AVAILABLE

    @NotNull(message = "Yeni əvəzedici texnika ID-si tələb olunur")
    private Long newEquipmentId;

    private Double newEquipmentInitialCounter;

    @NotNull(message = "Əvəzləmə tarixi tələb olunur")
    private LocalDate swapDate;

    @NotBlank(message = "Əvəzləmə səbəbi tələb olunur")
    private String swapReason;

    private String notes;
}
