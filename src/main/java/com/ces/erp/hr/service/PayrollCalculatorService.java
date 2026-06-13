package com.ces.erp.hr.service;

import com.ces.erp.hr.entity.PayrollEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aylıq əməkhaqqı hesablaması — generic {@link DeductionCalculator} motoru üzərində.
 *
 * <p>Bütün dərəcələr/hədlər DB-dən ({@link ResolvedDeductionConfig}) gəlir; bu sinifdə hardcoded
 * vergi düsturu yoxdur. Tutulma növləri kodlarına görə sabit {@link PayrollEntry} sütunlarına
 * map olunur (geriyə uyğunluq üçün).
 *
 * <p>Hesablama ardıcıllığı:
 * <ol>
 *   <li>Gross = (baseSalary × actualDays/workingDays) + overtimePay + bonus + vacationPay − penalty</li>
 *   <li>Bütün tutulmalar baza = gross üzərində hesablanır</li>
 *   <li>Net = gross − (net-dən çıxılan işçi tutulmaları cəmi)</li>
 *   <li>Şirkət xərci = gross + işəgötürən töhfələri</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PayrollCalculatorService {

    // Tutulma növü kodları → sabit PayrollEntry sütunları
    public static final String CODE_INCOME_TAX  = "GELIR_VERGISI";
    public static final String CODE_PENSION     = "DSMF";
    public static final String CODE_UNEMPLOYMENT = "ISH";
    public static final String CODE_MEDICAL     = "ITS";

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final DeductionCalculator deductionCalculator;

    /**
     * Bütün məbləğləri yenidən hesablayır və PayrollEntry-ni yeniləyir.
     */
    public void recalculate(PayrollEntry e, ResolvedDeductionConfig cfg) {
        BigDecimal baseSalary = nz(e.getBaseSalary());
        int workingDays = e.getWorkingDaysInMonth() != null && e.getWorkingDaysInMonth() > 0
                ? e.getWorkingDaysInMonth() : 22;
        int actualDays = e.getActualDaysWorked() != null ? e.getActualDaysWorked() : workingDays;
        BigDecimal overtimePay = nz(e.getOvertimePay());
        BigDecimal bonus = nz(e.getBonus());
        BigDecimal vacation = nz(e.getVacationPay());
        BigDecimal penalty = nz(e.getPenalty());

        // Faktiki günə görə əmək haqqı
        BigDecimal proRated;
        if (actualDays == workingDays) {
            proRated = baseSalary;
        } else {
            proRated = baseSalary
                    .multiply(BigDecimal.valueOf(actualDays))
                    .divide(BigDecimal.valueOf(workingDays), SCALE, ROUNDING);
        }

        BigDecimal gross = proRated.add(overtimePay).add(bonus).add(vacation).subtract(penalty);
        if (gross.signum() < 0) gross = BigDecimal.ZERO;
        gross = gross.setScale(SCALE, ROUNDING);

        DeductionCalculator.Result r = deductionCalculator.compute(gross, cfg.deductions());

        e.setGrossTotal(gross);

        // İşçidən tutulanlar (kodlara görə map)
        e.setIncomeTax(r.employee(CODE_INCOME_TAX));
        e.setEmployeePension(r.employee(CODE_PENSION));
        e.setEmployeeUnemployment(r.employee(CODE_UNEMPLOYMENT));
        e.setEmployeeMedical(r.employee(CODE_MEDICAL));
        e.setTotalDeductions(r.totalEmployeeDeductions());
        e.setNetPay(r.netPay());

        // İşəgötürən töhfələri
        e.setEmployerPension(r.employer(CODE_PENSION));
        e.setEmployerUnemployment(r.employer(CODE_UNEMPLOYMENT));
        e.setEmployerMedical(r.employer(CODE_MEDICAL));
        e.setTotalEmployerContributions(r.totalEmployerContributions());

        e.setTotalCompanyCost(round(gross.add(r.totalEmployerContributions())));
    }

    private BigDecimal round(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(SCALE, ROUNDING);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
