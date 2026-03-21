package com.ces.erp.approval.dto;

import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalSummaryResponse {

    private Long id;
    private String moduleCode;
    private String entityType;
    private Long entityId;
    private String entityLabel;
    private OperationType operationType;
    private OperationStatus status;

    private Long performedById;
    private String performedByName;
    private Long performerDepartmentId;
    private String performerDepartmentName;

    private Long processedById;
    private String processedByName;
    private LocalDateTime processedAt;
    private String rejectReason;

    private LocalDateTime createdAt;

    public static ApprovalSummaryResponse from(PendingOperation op) {
        return ApprovalSummaryResponse.builder()
                .id(op.getId())
                .moduleCode(op.getModuleCode())
                .entityType(op.getEntityType())
                .entityId(op.getEntityId())
                .entityLabel(op.getEntityLabel())
                .operationType(op.getOperationType())
                .status(op.getStatus())
                .performedById(op.getPerformedBy() != null ? op.getPerformedBy().getId() : null)
                .performedByName(op.getPerformedBy() != null ? op.getPerformedBy().getFullName() : null)
                .performerDepartmentId(op.getPerformerDepartment() != null ? op.getPerformerDepartment().getId() : null)
                .performerDepartmentName(op.getPerformerDepartment() != null ? op.getPerformerDepartment().getName() : null)
                .processedById(op.getProcessedBy() != null ? op.getProcessedBy().getId() : null)
                .processedByName(op.getProcessedBy() != null ? op.getProcessedBy().getFullName() : null)
                .processedAt(op.getProcessedAt())
                .rejectReason(op.getRejectReason())
                .createdAt(op.getCreatedAt())
                .build();
    }
}
