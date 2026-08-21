package com.ces.erp.project.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project_downtimes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDowntime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false, length = 50)
    private String reasonType; // WEATHER, CUSTOMER_SITE, TECHNICAL_BREAKDOWN, PAYMENT_DELAY, OTHER

    @Column(columnDefinition = "TEXT")
    private String reasonDescription;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPaid = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal standbyRate;

    @Column(nullable = false)
    @Builder.Default
    private boolean autoExtendEndDate = true;

    @Column(columnDefinition = "TEXT")
    private String resolvedNotes;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, RESOLVED
}
