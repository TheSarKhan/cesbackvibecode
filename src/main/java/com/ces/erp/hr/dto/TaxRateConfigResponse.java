package com.ces.erp.hr.dto;

import com.ces.erp.hr.entity.TaxRateConfig;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TaxRateConfigResponse {

    private Long id;
    private Integer year;
    private boolean active;

    private BigDecimal employeePensionThreshold;
    private BigDecimal employeePensionRateBelow;
    private BigDecimal employeePensionRateAbove;

    private BigDecimal employerPensionThreshold;
    private BigDecimal employerPensionRateBelow;
    private BigDecimal employerPensionRateAbove;

    private BigDecimal employeeUnemploymentRate;
    private BigDecimal employerUnemploymentRate;

    private BigDecimal employeeMedicalThreshold;
    private BigDecimal employeeMedicalRateBelow;
    private BigDecimal employeeMedicalRateAbove;

    private BigDecimal employerMedicalThreshold;
    private BigDecimal employerMedicalRateBelow;
    private BigDecimal employerMedicalRateAbove;

    private BigDecimal incomeTaxThreshold;
    private BigDecimal incomeTaxRateBelow;
    private BigDecimal incomeTaxRateAbove;

    private BigDecimal nonTaxableMinimum;
    private boolean deductSocialFromTaxBase;
    private String notes;

    public static TaxRateConfigResponse from(TaxRateConfig c) {
        return TaxRateConfigResponse.builder()
                .id(c.getId())
                .year(c.getYear())
                .active(c.isActive())
                .employeePensionThreshold(c.getEmployeePensionThreshold())
                .employeePensionRateBelow(c.getEmployeePensionRateBelow())
                .employeePensionRateAbove(c.getEmployeePensionRateAbove())
                .employerPensionThreshold(c.getEmployerPensionThreshold())
                .employerPensionRateBelow(c.getEmployerPensionRateBelow())
                .employerPensionRateAbove(c.getEmployerPensionRateAbove())
                .employeeUnemploymentRate(c.getEmployeeUnemploymentRate())
                .employerUnemploymentRate(c.getEmployerUnemploymentRate())
                .employeeMedicalThreshold(c.getEmployeeMedicalThreshold())
                .employeeMedicalRateBelow(c.getEmployeeMedicalRateBelow())
                .employeeMedicalRateAbove(c.getEmployeeMedicalRateAbove())
                .employerMedicalThreshold(c.getEmployerMedicalThreshold())
                .employerMedicalRateBelow(c.getEmployerMedicalRateBelow())
                .employerMedicalRateAbove(c.getEmployerMedicalRateAbove())
                .incomeTaxThreshold(c.getIncomeTaxThreshold())
                .incomeTaxRateBelow(c.getIncomeTaxRateBelow())
                .incomeTaxRateAbove(c.getIncomeTaxRateAbove())
                .nonTaxableMinimum(c.getNonTaxableMinimum())
                .deductSocialFromTaxBase(c.isDeductSocialFromTaxBase())
                .notes(c.getNotes())
                .build();
    }
}
