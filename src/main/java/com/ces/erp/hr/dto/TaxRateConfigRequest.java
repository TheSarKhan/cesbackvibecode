package com.ces.erp.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxRateConfigRequest {

    @NotNull
    private Integer year;

    private Boolean active;

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
    private Boolean deductSocialFromTaxBase;
    private String notes;
}
