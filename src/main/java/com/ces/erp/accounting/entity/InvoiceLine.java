package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Qaimə sətri — toplu qaimədə bir texnika. Bir qaimə çoxlu sətir daşıya bilər
 * (eyni layihənin texnikaları). Hər sətir öz tarifini, müddətini və məbləğini
 * saxlayır; qaimə məbləği sətirlərin cəmidir. Köhnə tək-texnikalı qaimələrdə
 * sətir olmaya bilər (Invoice.equipment/equipmentName geriyə uyğunluq).
 */
@Entity
@Table(name = "invoice_lines", indexes = {
        @Index(name = "idx_invoice_lines_invoice", columnList = "invoice_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private com.ces.erp.garage.entity.Equipment equipment;

    @Column(length = 255)
    private String equipmentName;

    // Hansı texnika xəttinə (CoordinatorPlanItem) aiddir — sahib həlli üçün
    private Long planItemId;

    // ─── Qiymət / müddət (hər sətir öz dövrünü daşıyır) ──────────────────────
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;     // tarif (gün/ay başına)

    private Integer dayCount;         // vahid sayı (gün və ya ay)

    // Aylıq iş cədvəli (sətir başına, könüllü)
    private Integer periodMonth;
    private Integer periodYear;
    private Integer standardDays;
    private Integer extraDays;
    @Column(precision = 8, scale = 2)
    private BigDecimal extraHours;
    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyRate;
    private Integer workingDaysInMonth;
    private Integer workingHoursPerDay;
    @Column(precision = 4, scale = 2)
    private BigDecimal overtimeRate;

    // ─── Məbləğlər ────────────────────────────────────────────────────────────
    @Column(precision = 12, scale = 2)
    private BigDecimal equipmentAmount;   // texnika məbləği (tarif × müddət + əlavə)

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal transportAmount = BigDecimal.ZERO;  // bu texnikanın daşınma cəmi

    @Column(precision = 12, scale = 2)
    private BigDecimal lineTotal;         // equipmentAmount + transportAmount

    // ─── Təhvil-təslim aktı (hər texnikanın öz akti) ─────────────────────────
    @Column(length = 500)
    private String aktFilePath;

    @Column(length = 255)
    private String aktFileName;
}
