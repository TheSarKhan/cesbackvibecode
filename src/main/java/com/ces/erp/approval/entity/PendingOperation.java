package com.ces.erp.approval.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.department.entity.Department;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingOperation extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String moduleCode;

    @Column(nullable = false, length = 100)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(length = 255)
    private String entityLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performer_department_id")
    private Department performerDepartment;

    @Column(columnDefinition = "TEXT")
    private String oldSnapshot;

    @Column(columnDefinition = "TEXT")
    private String newSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OperationStatus status = OperationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    private LocalDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;
}
