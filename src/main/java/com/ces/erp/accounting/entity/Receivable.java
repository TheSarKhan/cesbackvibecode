package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.ReceivableStatus;
import com.ces.erp.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receivables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receivable extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

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
    private ReceivableStatus status = ReceivableStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "receivable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReceivablePayment> payments = new ArrayList<>();
}
