package com.ces.erp.hr.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Yeni versiya yaratma sorğusu. Hər dəyişiklik yeni versiya kimi saxlanılır.
 * {@code groups} içində hər element mövcud tutulma növünə ({@code deductionTypeId}) istinad edir.
 */
public record CreateVersionRequest(
        @NotNull LocalDate effectiveDate,
        Boolean active,
        String note,
        @NotNull List<DeductionGroupDto> groups) {
}
