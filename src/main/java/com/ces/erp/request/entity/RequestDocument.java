package com.ces.erp.request.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.coordinator.entity.CoordinatorPlanItem;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_documents", indexes = {
        @Index(name = "idx_reqdoc_request", columnList = "request_id"),
        @Index(name = "idx_reqdoc_type", columnList = "docType"),
        @Index(name = "idx_reqdoc_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private TechRequest request;

    // Çoxlu texnika: sənəd konkret texnika xəttinə bağlana bilər (null = sorğu səviyyəsində / köhnə)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_item_id")
    private CoordinatorPlanItem planItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestDocumentType docType;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;
}
