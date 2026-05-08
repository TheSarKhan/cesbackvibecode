package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Aylıq əməkhaqqı vergisi/sığorta tarifləri (illik konfiqurasiya).
 *
 * Azərbaycan qeyri-neft-qaz, qeyri-dövlət sektoru üçün default 2026:
 *   - Pensiya (işçi):     ilk 200 AZN-ə 3%, üstündə 10%
 *   - Pensiya (işəgötürən): ilk 200 AZN-ə 22%, üstündə 15%
 *   - İşsizlik (işçi):     0.5%
 *   - İşsizlik (işəgötürən): 0.5%
 *   - Tibbi sığorta (işçi): ilk 8000-ə 2%, üstündə 0.5%
 *   - Tibbi sığorta (işəgötürən): ilk 8000-ə 2%, üstündə 0.5%
 *   - Gəlir vergisi: ilk 8000 AZN-ə 0%, üstündə 14% (qeyri-neft-qaz, qeyri-dövlət sektoru üçün)
 *
 * Hər il ayrı bir TaxRateConfig saxlanılır. Aktiv olan biridir (active=true).
 */
@Entity
@Table(name = "hr_tax_rate_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tax_rate_year", columnNames = {"effective_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRateConfig extends BaseEntity {

    @Column(name = "effective_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Pensiya Fondu — işçi ──
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal employeePensionThreshold = new BigDecimal("200.00");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employeePensionRateBelow = new BigDecimal("0.03");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employeePensionRateAbove = new BigDecimal("0.10");

    // ── Pensiya Fondu — işəgötürən ──
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal employerPensionThreshold = new BigDecimal("200.00");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employerPensionRateBelow = new BigDecimal("0.22");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employerPensionRateAbove = new BigDecimal("0.15");

    // ── İşsizlik Sığortası ──
    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employeeUnemploymentRate = new BigDecimal("0.005");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employerUnemploymentRate = new BigDecimal("0.005");

    // ── Tibbi Sığorta — işçi ──
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal employeeMedicalThreshold = new BigDecimal("2500.00");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employeeMedicalRateBelow = new BigDecimal("0.02");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employeeMedicalRateAbove = new BigDecimal("0.005");

    // ── Tibbi Sığorta — işəgötürən ──
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal employerMedicalThreshold = new BigDecimal("2500.00");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employerMedicalRateBelow = new BigDecimal("0.02");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal employerMedicalRateAbove = new BigDecimal("0.005");

    // ── Gəlir Vergisi ──
    // Qeyri-neft-qaz, qeyri-dövlət sektoru üçün: ilk 8000 AZN-ə 0%, üstündə 14%
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal incomeTaxThreshold = new BigDecimal("8000.00");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal incomeTaxRateBelow = new BigDecimal("0.00");

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal incomeTaxRateAbove = new BigDecimal("0.14");

    // Qeyri-vergi minimumu (lazımdırsa, gəlir vergisi base-dan çıxılır)
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal nonTaxableMinimum = BigDecimal.ZERO;

    // Sosial töhfələr gəlir vergisi base-ından çıxılırmı?
    @Column(nullable = false)
    @Builder.Default
    private boolean deductSocialFromTaxBase = false;

    @Column(length = 500)
    private String notes;
}
