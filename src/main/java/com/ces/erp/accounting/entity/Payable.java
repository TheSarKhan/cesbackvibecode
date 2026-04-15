package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.enums.PayableStatus;
import com.ces.erp.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payable extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    // CONTRACTOR ownership üçün — Contractor entity linki
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    // INVESTOR ownership üçün — plain string (entity yoxdur)
    @Column(length = 255)
    private String investorName;

    @Column(length = 20)
    private String investorVoen;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PayableStatus status = PayableStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "payable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayablePayment> payments = new ArrayList<>();
}
