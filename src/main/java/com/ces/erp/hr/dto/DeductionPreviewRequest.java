package com.ces.erp.hr.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canlı önizləmə sorğusu — yadda SAXLANMAMIŞ (draft) aralıqları qəbul edir.
 * İstifadəçi popup-da dəyər dəyişdikcə saxlamadan əvvəl nəticəni görür.
 * Hər qrupda {@code code/appliesTo/deductedFromNet} inline verilir (aktiv versiyadan oxunmur).
 */
public record DeductionPreviewRequest(
        @NotNull BigDecimal base,
        @NotNull List<DeductionGroupDto> groups) {
}
