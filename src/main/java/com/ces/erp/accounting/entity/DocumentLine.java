package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "document_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private GeneratedDocument document;

    @Column(nullable = false)
    private int lineOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;     // "Komatsu PC 450 ekskavatoru icarəsi"

    @Column(nullable = false, length = 50)
    private String unit;            // "gün", "ədəd", "dəfə"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;  // quantity × unitPrice

    private Long sourceInvoiceId;   // hansı qaimədən yaranıb (nullable)
}
