package com.ces.erp.hr.service;

import com.ces.erp.hr.entity.PayrollEntry;
import com.ces.erp.hr.entity.TaxRateConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aylıq əməkhaqqı hesablama düsturları.
 *
 * <p>Hesablama ardıcıllığı bir PayrollEntry üzərində:
 * <ol>
 *   <li>Gross total = (baseSalary × actualDays/workingDays) + overtimePay + bonus + vacationPay - penalty</li>
 *   <li>İşçidən tutulanlar: pensiya, işsizlik, tibbi sığorta (gross əsasında, threshold-lu)</li>
 *   <li>Gəlir vergisi base: gross (və ya gross - sosial töhfələr - qeyri-vergi minimumu)</li>
 *   <li>Gəlir vergisi: threshold-dan yuxarı hissəyə tətbiq olunur</li>
 *   <li>Net = gross - cəmi tutulmuşdur</li>
 *   <li>İşəgötürən töhfələri: pensiya, işsizlik, tibbi (eyni qaydada)</li>
 *   <li>Şirkət cəmi xərci = gross + employer contributions</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PayrollCalculatorService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Bütün məbləğləri yenidən hesablayır və PayrollEntry-ni yeniləyir.
     */
    public void recalculate(PayrollEntry e, TaxRateConfig cfg) {
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

        // İşçidən tutulanlar (xam dəyərlər — yuvarlaqlaşdırma yalnız sonda)
        BigDecimal employeePension = bracketedRaw(gross,
                cfg.getEmployeePensionThreshold(),
                cfg.getEmployeePensionRateBelow(),
                cfg.getEmployeePensionRateAbove());

        BigDecimal employeeUnemployment = gross.multiply(cfg.getEmployeeUnemploymentRate());

        BigDecimal employeeMedical = bracketedRaw(gross,
                cfg.getEmployeeMedicalThreshold(),
                cfg.getEmployeeMedicalRateBelow(),
                cfg.getEmployeeMedicalRateAbove());

        // Gəlir vergisi
        BigDecimal taxBase = gross;
        if (cfg.isDeductSocialFromTaxBase()) {
            taxBase = taxBase.subtract(employeePension)
                    .subtract(employeeUnemployment)
                    .subtract(employeeMedical);
        }
        if (cfg.getNonTaxableMinimum() != null && cfg.getNonTaxableMinimum().signum() > 0) {
            taxBase = taxBase.subtract(cfg.getNonTaxableMinimum());
        }
        if (taxBase.signum() < 0) taxBase = BigDecimal.ZERO;

        BigDecimal incomeTax = progressiveIncomeTaxRaw(taxBase);

        // Cəmi tutulmuş və net — xam dəyərlərin cəmindən hesablanır,
        // sonra yuvarlaqlaşdırılır (kumulyativ 0.01 fərq olmasın deyə)
        BigDecimal totalDeductionsRaw = incomeTax
                .add(employeePension)
                .add(employeeUnemployment)
                .add(employeeMedical);

        BigDecimal netPayRaw = gross.subtract(totalDeductionsRaw);
        if (netPayRaw.signum() < 0) netPayRaw = BigDecimal.ZERO;

        // İşəgötürən töhfələri
        BigDecimal employerPension = bracketedRaw(gross,
                cfg.getEmployerPensionThreshold(),
                cfg.getEmployerPensionRateBelow(),
                cfg.getEmployerPensionRateAbove());

        BigDecimal employerUnemployment = gross.multiply(cfg.getEmployerUnemploymentRate());

        BigDecimal employerMedical = bracketedRaw(gross,
                cfg.getEmployerMedicalThreshold(),
                cfg.getEmployerMedicalRateBelow(),
                cfg.getEmployerMedicalRateAbove());

        BigDecimal totalEmployerRaw = employerPension
                .add(employerUnemployment)
                .add(employerMedical);

        BigDecimal totalCompanyCostRaw = gross.add(totalEmployerRaw);

        e.setGrossTotal(gross);
        e.setIncomeTax(round(incomeTax));
        e.setEmployeePension(round(employeePension));
        e.setEmployeeUnemployment(round(employeeUnemployment));
        e.setEmployeeMedical(round(employeeMedical));
        e.setTotalDeductions(round(totalDeductionsRaw));
        e.setNetPay(round(netPayRaw));
        e.setEmployerPension(round(employerPension));
        e.setEmployerUnemployment(round(employerUnemployment));
        e.setEmployerMedical(round(employerMedical));
        e.setTotalEmployerContributions(round(totalEmployerRaw));
        e.setTotalCompanyCost(round(totalCompanyCostRaw));
    }

    private BigDecimal round(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(SCALE, ROUNDING);
    }

    /**
     * Azərbaycan 2026 progressiv gəlir vergisi (qeyri-neft-qaz, qeyri-dövlət sektoru):
     *   ≤ 200 AZN        → 0
     *   ≤ 2500 AZN       → (taxBase − 200) × 3%
     *   ≤ 8000 AZN       → 75 + (taxBase − 2500) × 10%
     *   > 8000 AZN       → 625 + (taxBase − 8000) × 14%
     */
    private BigDecimal progressiveIncomeTaxRaw(BigDecimal taxBase) {
        if (taxBase == null || taxBase.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal b1 = new BigDecimal("200");
        BigDecimal b2 = new BigDecimal("2500");
        BigDecimal b3 = new BigDecimal("8000");

        if (taxBase.compareTo(b1) <= 0) {
            return BigDecimal.ZERO;
        } else if (taxBase.compareTo(b2) <= 0) {
            return taxBase.subtract(b1).multiply(new BigDecimal("0.03"));
        } else if (taxBase.compareTo(b3) <= 0) {
            return new BigDecimal("75")
                    .add(taxBase.subtract(b2).multiply(new BigDecimal("0.10")));
        } else {
            return new BigDecimal("625")
                    .add(taxBase.subtract(b3).multiply(new BigDecimal("0.14")));
        }
    }

    /**
     * Bracketed (threshold) hesablama:
     *   amount ≤ threshold:           amount × rateBelow
     *   amount > threshold:           threshold × rateBelow + (amount - threshold) × rateAbove
     */
    private BigDecimal bracketedRaw(BigDecimal amount, BigDecimal threshold, BigDecimal rateBelow, BigDecimal rateAbove) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal t = threshold == null ? BigDecimal.ZERO : threshold;
        BigDecimal rb = rateBelow == null ? BigDecimal.ZERO : rateBelow;
        BigDecimal ra = rateAbove == null ? BigDecimal.ZERO : rateAbove;

        if (amount.compareTo(t) <= 0) {
            return amount.multiply(rb);
        }
        BigDecimal belowPart = t.multiply(rb);
        BigDecimal abovePart = amount.subtract(t).multiply(ra);
        return belowPart.add(abovePart);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
