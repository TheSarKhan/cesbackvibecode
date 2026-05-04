package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.PayrollStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hr_payroll_periods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_period_year_month", columnNames = {"period_year", "period_month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollPeriod extends BaseEntity {

    @Column(name = "period_year", nullable = false)
    private Integer year;

    @Column(name = "period_month", nullable = false)
    private Integer month;

    // Bu ayın iş günü sayı (təsdiqlənən zaman istifadə olunur, default 22)
    @Column(nullable = false)
    @Builder.Default
    private Integer workingDaysInMonth = 22;

    // Standart gündəlik iş saatları (default 8)
    @Column(nullable = false)
    @Builder.Default
    private Integer workingHoursPerDay = 8;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalEmployeeDeductions = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalEmployerContributions = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalIncomeTax = BigDecimal.ZERO;

    private LocalDateTime approvedAt;

    private String approvedBy;

    private LocalDateTime paidAt;

    @Column(length = 500)
    private String notes;

    // Bağlı qaimə id-si (mühasibatlığa göndərildikdə)
    private Long invoiceId;

    @OneToMany(mappedBy = "period", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayrollEntry> entries = new ArrayList<>();
}
