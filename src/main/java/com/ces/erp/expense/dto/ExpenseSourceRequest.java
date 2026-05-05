package com.ces.erp.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseSourceRequest {

    @NotBlank(message = "Kod boş ola bilməz")
    private String code;

    @NotBlank(message = "Ad boş ola bilməz")
    private String name;

    @NotNull(message = "Kateqoriya seçilməlidir")
    private Long categoryId;

    private boolean active = true;
}
