package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.RecurringExpense;
import com.ces.erp.enums.RecurrenceFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecurringExpenseResponse {

    private Long id;
    private String name;

    private String categoryKey;
    private String categoryLabel;
    private String sourceKey;
    private String sourceLabel;

    private BigDecimal amount;
    private boolean variableAmount;     // amount == 0

    private RecurrenceFrequency frequency;
    private String frequencyLabel;

    private Integer dayOfMonth;
    private String notes;
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecurringExpenseResponse from(RecurringExpense e) {
        String freqLabel = switch (e.getFrequency()) {
            case MONTHLY   -> "Aylıq";
            case QUARTERLY -> "Rüblük";
            case ANNUAL    -> "İllik";
        };
        return RecurringExpenseResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .categoryKey(e.getCategoryKey())
                .categoryLabel(e.getCategoryLabel())
                .sourceKey(e.getSourceKey())
                .sourceLabel(e.getSourceLabel())
                .amount(e.getAmount())
                .variableAmount(e.getAmount().compareTo(BigDecimal.ZERO) == 0)
                .frequency(e.getFrequency())
                .frequencyLabel(freqLabel)
                .dayOfMonth(e.getDayOfMonth())
                .notes(e.getNotes())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
