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

        // İşçidən tutulanlar
        BigDecimal employeePension = bracketed(gross,
                cfg.getEmployeePensionThreshold(),
                cfg.getEmployeePensionRateBelow(),
                cfg.getEmployeePensionRateAbove());

        BigDecimal employeeUnemployment = gross
                .multiply(cfg.getEmployeeUnemploymentRate())
                .setScale(SCALE, ROUNDING);

        BigDecimal employeeMedical = bracketed(gross,
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

        BigDecimal incomeTax = bracketed(taxBase,
                cfg.getIncomeTaxThreshold(),
                cfg.getIncomeTaxRateBelow(),
                cfg.getIncomeTaxRateAbove());

        BigDecimal totalDeductions = incomeTax
                .add(employeePension)
                .add(employeeUnemployment)
                .add(employeeMedical)
                .setScale(SCALE, ROUNDING);

        BigDecimal netPay = gross.subtract(totalDeductions);
        if (netPay.signum() < 0) netPay = BigDecimal.ZERO;

        // İşəgötürən töhfələri
        BigDecimal employerPension = bracketed(gross,
                cfg.getEmployerPensionThreshold(),
                cfg.getEmployerPensionRateBelow(),
                cfg.getEmployerPensionRateAbove());

        BigDecimal employerUnemployment = gross
                .multiply(cfg.getEmployerUnemploymentRate())
                .setScale(SCALE, ROUNDING);

        BigDecimal employerMedical = bracketed(gross,
                cfg.getEmployerMedicalThreshold(),
                cfg.getEmployerMedicalRateBelow(),
                cfg.getEmployerMedicalRateAbove());

        BigDecimal totalEmployer = employerPension
                .add(employerUnemployment)
                .add(employerMedical)
                .setScale(SCALE, ROUNDING);

        BigDecimal totalCompanyCost = gross.add(totalEmployer);

        e.setGrossTotal(gross);
        e.setIncomeTax(incomeTax);
        e.setEmployeePension(employeePension);
        e.setEmployeeUnemployment(employeeUnemployment);
        e.setEmployeeMedical(employeeMedical);
        e.setTotalDeductions(totalDeductions);
        e.setNetPay(netPay.setScale(SCALE, ROUNDING));
        e.setEmployerPension(employerPension);
        e.setEmployerUnemployment(employerUnemployment);
        e.setEmployerMedical(employerMedical);
        e.setTotalEmployerContributions(totalEmployer);
        e.setTotalCompanyCost(totalCompanyCost.setScale(SCALE, ROUNDING));
    }

    /**
     * Bracketed (threshold) hesablama:
     *   amount ≤ threshold:           amount × rateBelow
     *   amount > threshold:           threshold × rateBelow + (amount - threshold) × rateAbove
     */
    private BigDecimal bracketed(BigDecimal amount, BigDecimal threshold, BigDecimal rateBelow, BigDecimal rateAbove) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal t = threshold == null ? BigDecimal.ZERO : threshold;
        BigDecimal rb = rateBelow == null ? BigDecimal.ZERO : rateBelow;
        BigDecimal ra = rateAbove == null ? BigDecimal.ZERO : rateAbove;

        if (amount.compareTo(t) <= 0) {
            return amount.multiply(rb).setScale(SCALE, ROUNDING);
        }
        BigDecimal belowPart = t.multiply(rb);
        BigDecimal abovePart = amount.subtract(t).multiply(ra);
        return belowPart.add(abovePart).setScale(SCALE, ROUNDING);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
