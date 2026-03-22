package com.ces.erp.request.dto;

import com.ces.erp.request.entity.RequestStatusLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusLogResponse {

    private Long id;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;

    public static StatusLogResponse from(RequestStatusLog log) {
        return StatusLogResponse.builder()
                .id(log.getId())
                .oldStatus(log.getOldStatus().name())
                .newStatus(log.getNewStatus().name())
                .reason(log.getReason())
                .changedBy(log.getChangedBy())
                .changedAt(log.getChangedAt())
                .build();
    }
}
