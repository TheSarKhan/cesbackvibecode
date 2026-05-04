package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hr_payroll_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_entry_period_employee", columnNames = {"period_id", "employee_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private PayrollPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Snapshot — dövr bağlanandan sonra dəyişməsin deyə
    @Column(nullable = false)
    private String employeeFullName;

    private String positionName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @Column(nullable = false)
    @Builder.Default
    private Integer workingDaysInMonth = 22;

    @Column(nullable = false)
    @Builder.Default
    private Integer actualDaysWorked = 22;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal extraHours = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal vacationPay = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal penalty = BigDecimal.ZERO;

    // ── HESABLANIB CƏMİ (gross + bonus + vacation - penalty) ──
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal grossTotal = BigDecimal.ZERO;

    // ── İşçidən tutulanlar ──
    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal incomeTax = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal employeePension = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal employeeUnemployment = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal employeeMedical = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // ── Net (Ödənilməli məbləğ) ──
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal netPay = BigDecimal.ZERO;

    // ── İşəgötürən tərəfindən (şirkət xərci əlavə) ──
    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal employerPension = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal employerUnemployment = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal employerMedical = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalEmployerContributions = BigDecimal.ZERO;

    // Şirkət üçün toplam xərc = gross + employer contributions
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalCompanyCost = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayrollAdjustment> adjustments = new ArrayList<>();
}
