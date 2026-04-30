package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.RecurrenceFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recurring_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExpense extends BaseEntity {

    // ─── Əsas məlumat ─────────────────────────────────────────────────────────

    @Column(nullable = false, length = 200)
    private String name;                 // Məsələn: "Azercell Korporativ İnternet"

    // ─── Kateqoriya → Mənbə ───────────────────────────────────────────────────

    @Column(nullable = false, length = 100)
    private String categoryKey;          // EXPENSE_CATEGORY item key, məs: "PROVIDER"

    @Column(nullable = false, length = 200)
    private String categoryLabel;        // Display adı, məs: "Provayder"

    @Column(nullable = false, length = 100)
    private String sourceKey;            // EXPENSE_SOURCE item key, məs: "AZERCELL"

    @Column(nullable = false, length = 200)
    private String sourceLabel;          // Display adı, məs: "Azercell"

    // ─── Məbləğ ───────────────────────────────────────────────────────────────

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;   // 0 = dəyişkən, hər dəfə daxil edilir

    // ─── Təkrarlama ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecurrenceFrequency frequency = RecurrenceFrequency.MONTHLY;

    private Integer dayOfMonth;          // Ödəniş günü (1-31), null = müəyyən deyil

    // ─── Əlavə ────────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
