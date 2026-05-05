package com.ces.erp.expense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseCategoryRequest {

    @NotBlank(message = "Kod boş ola bilməz")
    private String code;

    @NotBlank(message = "Ad boş ola bilməz")
    private String name;

    private String description;

    private boolean active = true;
}
