package com.ces.erp.hr.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Müəyyən bir tarix üçün həll olunmuş (resolved) tutulma konfiqurasiyası —
 * hesablama motoruna ötürülən hazır forma.
 */
public record ResolvedDeductionConfig(
        Long versionId,
        Integer versionNo,
        LocalDate effectiveDate,
        List<DeductionCalculator.DeductionDef> deductions) {
}
