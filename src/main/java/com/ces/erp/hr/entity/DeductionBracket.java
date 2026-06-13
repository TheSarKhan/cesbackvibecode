package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.DeductionParty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Bir versiya × tutulma növü × taraf üçün maaş aralığı (dilim).
 *
 * <p>Hesablama düsturu (dilimə düşən baza üçün):
 * <pre>sabit_mebleg + (baza − alt_hedd) × faiz − güzəşt</pre>
 * Sərhəd: {@code alt_hedd < baza ≤ ust_hedd} (ust_hedd NULL = sonsuz).
 */
@Entity
@Table(name = "hr_deduction_bracket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionBracket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DeductionConfigVersion version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deduction_type_id", nullable = false)
    private DeductionType deductionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeductionParty party;

    @Column(name = "lower_bound", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal lowerBound = BigDecimal.ZERO;

    /** NULL = sonsuz (ən yuxarı dilim). */
    @Column(name = "upper_bound", precision = 14, scale = 4)
    private BigDecimal upperBound;

    @Column(name = "fixed_amount", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 6)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
