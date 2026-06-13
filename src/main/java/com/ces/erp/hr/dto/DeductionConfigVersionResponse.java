package com.ces.erp.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Versiya cavabı. {@code groups} yalnız detal (tək versiya) sorğularında doldurulur;
 * siyahı sorğusunda null ola bilər.
 */
public record DeductionConfigVersionResponse(
        Long id,
        Integer versionNo,
        LocalDate effectiveDate,
        boolean active,
        String createdBy,
        String note,
        LocalDateTime createdAt,
        List<DeductionGroupDto> groups) {
}
