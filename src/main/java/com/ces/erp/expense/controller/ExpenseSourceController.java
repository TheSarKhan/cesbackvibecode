package com.ces.erp.expense.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.expense.dto.ExpenseSourceRequest;
import com.ces.erp.expense.dto.ExpenseSourceResponse;
import com.ces.erp.expense.service.ExpenseSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense-sources")
@RequiredArgsConstructor
@Tag(name = "Expense Sources", description = "Xərc mənbələri")
public class ExpenseSourceController {

    private final ExpenseSourceService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Bütün xərc mənbələrini gətir")
    public ResponseEntity<ApiResponse<List<ExpenseSourceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Aktiv xərc mənbələrini gətir")
    public ResponseEntity<ApiResponse<List<ExpenseSourceResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllActive()));
    }

    @GetMapping("/by-category/{categoryId}")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Kateqoriyaya görə mənbələri gətir")
    public ResponseEntity<ApiResponse<List<ExpenseSourceResponse>>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByCategory(categoryId)));
    }

    @GetMapping("/by-category/{categoryId}/active")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Kateqoriyaya görə aktiv mənbələri gətir")
    public ResponseEntity<ApiResponse<List<ExpenseSourceResponse>>> getActiveByCategoryId(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(service.getActiveByCategoryId(categoryId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "ID ilə gətir")
    public ResponseEntity<ApiResponse<ExpenseSourceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG:POST')")
    @Operation(summary = "Yeni xərc mənbəyi yarat")
    public ResponseEntity<ApiResponse<ExpenseSourceResponse>> create(@Valid @RequestBody ExpenseSourceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Mənbə yaradıldı", service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:PUT')")
    @Operation(summary = "Xərc mənbəyini yenilə")
    public ResponseEntity<ApiResponse<ExpenseSourceResponse>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody ExpenseSourceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Mənbə yeniləndi", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:DELETE')")
    @Operation(summary = "Xərc mənbəyini sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Mənbə silindi"));
    }
}
