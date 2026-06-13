package com.ces.erp.hr.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canlı önizləmə nəticəsi.
 */
public record DeductionPreviewResponse(
        BigDecimal base,
        List<Line> lines,
        BigDecimal totalEmployeeDeductions,
        BigDecimal totalEmployerContributions,
        BigDecimal netPay) {

    public record Line(
            String code,
            String name,
            boolean deductedFromNet,
            BigDecimal employeeAmount,
            BigDecimal employerAmount) {
    }
}
