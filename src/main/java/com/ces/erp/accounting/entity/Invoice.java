package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    // ─── Növ ──────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;   // DRAFT, SENT

    // ─── Əsas sahələr ─────────────────────────────────────────────────────────

    @Column(length = 50)
    private String invoiceNumber;       // Qaimə nömrəsi (sonradan verilə bilər)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;          // Qaimə məbləği

    @Column(nullable = false)
    private LocalDate invoiceDate;      // Qaimənin tarixi

    // ─── Type A — Gəlir qaiməsi ───────────────────────────────────────────────

    private String etaxesId;            // ETaxes platformasından ID

    // ─── Type A / B1 — Texnika ────────────────────────────────────────────────

    private String equipmentName;       // Xidmət göstərilən texnika

    // ─── Type A — Müştəri şirkəti / B2 — Xidmət göstərən şirkət ──────────────

    private String companyName;

    // ─── Type B2 — Xidmət növü ────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String serviceDescription;

    // ─── Əlaqələr ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;            // A və B1 üçün məcburi, B2 üçün könüllü

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;      // B1 üçün məcburi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;          // İstəyə bağlı

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ─── Aylıq iş cədvəli (INCOME növü üçün) ─────────────────────────────────

    @Column
    private Integer periodMonth;          // 1–12

    @Column
    private Integer periodYear;           // 2025, 2026…

    @Column
    private Integer standardDays;         // standart iş günü sayı

    @Column
    private Integer extraDays;            // əlavə gün sayı

    @Column(precision = 8, scale = 2)
    private BigDecimal extraHours;        // əlavə saatlar (onluq: 1.5)

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyRate;       // aylıq tarif (məs. 14000)

    @Column
    private Integer workingDaysInMonth;   // aylıq iş günü norması (default 26)

    @Column
    private Integer workingHoursPerDay;   // gündəlik iş saatı (default 9)

    @Column(precision = 4, scale = 2)
    private BigDecimal overtimeRate;      // əlavə saat dərəcəsi (1.0 = adi, 1.5 = əlavə)
}
