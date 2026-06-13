package com.ces.erp.hr.dto;

import com.ces.erp.enums.DeductionAppliesTo;

import java.util.List;

/**
 * Bir tutulma növü + onun işçi/işəgötürən aralıqları.
 *
 * <p>İstifadə yerləri:
 * <ul>
 *   <li>Versiya cavabı — bütün sahələr doludur</li>
 *   <li>Versiya yaratma — {@code deductionTypeId} ilə mövcud növə istinad + aralıqlar</li>
 *   <li>Canlı önizləmə (preview) — {@code code/appliesTo/deductedFromNet} inline (draft, hələ saxlanmamış)</li>
 * </ul>
 */
public record DeductionGroupDto(
        Long deductionTypeId,
        String code,
        String name,
        DeductionAppliesTo appliesTo,
        Boolean deductedFromNet,
        Integer displayOrder,
        Boolean active,
        List<BracketDto> isciBrackets,
        List<BracketDto> isegoturenBrackets) {
}
