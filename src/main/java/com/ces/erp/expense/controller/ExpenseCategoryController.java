package com.ces.erp.expense.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.expense.dto.ExpenseCategoryRequest;
import com.ces.erp.expense.dto.ExpenseCategoryResponse;
import com.ces.erp.expense.service.ExpenseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories", description = "Xərc kateqoriyaları")
public class ExpenseCategoryController {

    private final ExpenseCategoryService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Bütün xərc kateqoriyalarını gətir")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Aktiv xərc kateqoriyalarını gətir")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "ID ilə gətir")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG:POST')")
    @Operation(summary = "Yeni xərc kateqoriyası yarat")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> create(@Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Kateqoriya yaradıldı", service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:PUT')")
    @Operation(summary = "Xərc kateqoriyasını yenilə")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> update(@PathVariable Long id,
                                                                        @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Kateqoriya yeniləndi", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:DELETE')")
    @Operation(summary = "Xərc kateqoriyasını sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Kateqoriya silindi"));
    }
}
