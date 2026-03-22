package com.ces.erp.garage.dto;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.garage.entity.EquipmentStatusLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusLogResponse {

    private Long id;
    private EquipmentStatus oldStatus;
    private EquipmentStatus newStatus;
    private String reason;
    private Long changedByUserId;
    private String changedByUserName;
    private LocalDateTime changedAt;

    public static StatusLogResponse from(EquipmentStatusLog log) {
        return StatusLogResponse.builder()
                .id(log.getId())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .reason(log.getReason())
                .changedByUserId(log.getChangedBy() != null ? log.getChangedBy().getId() : null)
                .changedByUserName(log.getChangedBy() != null ? log.getChangedBy().getFullName() : null)
                .changedAt(log.getChangedAt())
                .build();
    }
}
