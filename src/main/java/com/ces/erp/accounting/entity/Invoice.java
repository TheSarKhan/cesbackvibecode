package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.enums.InvoiceType;
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

    @Column(columnDefinition = "TEXT")
    private String notes;
}
