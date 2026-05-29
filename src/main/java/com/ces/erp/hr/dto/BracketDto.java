package com.ces.erp.hr.dto;

import java.math.BigDecimal;

/**
 * Bir maaş aralığı (dilim). upperBound null = sonsuz.
 */
public record BracketDto(
        Long id,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        BigDecimal fixedAmount,
        BigDecimal rate,
        Integer sortOrder) {
}
