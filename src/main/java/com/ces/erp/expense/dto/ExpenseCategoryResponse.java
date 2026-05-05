package com.ces.erp.expense.dto;

import com.ces.erp.expense.entity.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseCategoryResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    public static ExpenseCategoryResponse from(ExpenseCategory c) {
        return ExpenseCategoryResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
