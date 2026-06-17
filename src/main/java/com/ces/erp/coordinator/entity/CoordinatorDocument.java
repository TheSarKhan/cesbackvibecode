package com.ces.erp.coordinator.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coordinator_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinatorDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private CoordinatorPlan plan;

    // Sənəd konkret texnika xəttinə aiddirsə (məs. təhvil-təslim aktı) — opsional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_item_id")
    private CoordinatorPlanItem planItem;

    private String documentName;

    @Column(nullable = false)
    private String filePath;

    @Column(length = 20)
    private String fileType;

    // REGISTRATION_CERT | THIRD_PARTY_INSPECTION | TECHNICAL_INSPECTION | OTHER
    @Column(length = 50)
    private String documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;
}
