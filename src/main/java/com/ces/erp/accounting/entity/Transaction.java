package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounting_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String type;           // INCOME | EXPENSE

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 100)
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long projectId;

    private Long contractorId;

    private Long customerId;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
