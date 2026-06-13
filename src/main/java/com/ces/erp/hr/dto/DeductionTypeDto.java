package com.ces.erp.hr.dto;

import com.ces.erp.enums.DeductionAppliesTo;
import com.ces.erp.hr.entity.DeductionType;

/**
 * Tutulma növü — CRUD üçün.
 */
public record DeductionTypeDto(
        Long id,
        String code,
        String name,
        DeductionAppliesTo appliesTo,
        Boolean deductedFromNet,
        Integer displayOrder,
        Boolean active) {

    public static DeductionTypeDto from(DeductionType t) {
        return new DeductionTypeDto(t.getId(), t.getCode(), t.getName(), t.getAppliesTo(),
                t.isDeductedFromNet(), t.getDisplayOrder(), t.isActive());
    }
}
