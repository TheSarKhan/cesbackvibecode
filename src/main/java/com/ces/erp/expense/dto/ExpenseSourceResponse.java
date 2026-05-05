package com.ces.erp.expense.dto;

import com.ces.erp.expense.entity.ExpenseSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseSourceResponse {

    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private boolean active;
    private LocalDateTime createdAt;

    public static ExpenseSourceResponse from(ExpenseSource s) {
        return ExpenseSourceResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .categoryId(s.getCategory().getId())
                .categoryCode(s.getCategory().getCode())
                .categoryName(s.getCategory().getName())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
