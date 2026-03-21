package com.ces.erp.approval.dto;

import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PendingOperationResponse {

    private Long id;
    private String moduleCode;
    private String entityType;
    private Long entityId;
    private String entityLabel;
    private OperationType operationType;
    private OperationStatus status;

    private Long performedById;
    private String performedByName;
    private String performerDepartmentName;

    private Long processedById;
    private String processedByName;
    private LocalDateTime processedAt;
    private String rejectReason;

    private Map<String, Object> oldSnapshot;
    private Map<String, Object> newSnapshot;

    private LocalDateTime createdAt;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    public static PendingOperationResponse from(PendingOperation op) {
        return PendingOperationResponse.builder()
                .id(op.getId())
                .moduleCode(op.getModuleCode())
                .entityType(op.getEntityType())
                .entityId(op.getEntityId())
                .entityLabel(op.getEntityLabel())
                .operationType(op.getOperationType())
                .status(op.getStatus())
                .performedById(op.getPerformedBy() != null ? op.getPerformedBy().getId() : null)
                .performedByName(op.getPerformedBy() != null ? op.getPerformedBy().getFullName() : null)
                .performerDepartmentName(op.getPerformerDepartment() != null ? op.getPerformerDepartment().getName() : null)
                .processedById(op.getProcessedBy() != null ? op.getProcessedBy().getId() : null)
                .processedByName(op.getProcessedBy() != null ? op.getProcessedBy().getFullName() : null)
                .processedAt(op.getProcessedAt())
                .rejectReason(op.getRejectReason())
                .oldSnapshot(parseJson(op.getOldSnapshot()))
                .newSnapshot(parseJson(op.getNewSnapshot()))
                .createdAt(op.getCreatedAt())
                .build();
    }

    private static Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
