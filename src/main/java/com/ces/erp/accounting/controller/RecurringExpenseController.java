package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.*;
import com.ces.erp.accounting.service.RecurringExpenseService;
import com.ces.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/recurring")
@RequiredArgsConstructor
@Tag(name = "Recurring Expenses", description = "Daimi (təkrarlanan) ödənişlər")
public class RecurringExpenseController {

    private final RecurringExpenseService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Bütün daimi ödənişləri gətir")
    public ResponseEntity<ApiResponse<List<RecurringExpenseResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Aktiv daimi ödənişləri gətir")
    public ResponseEntity<ApiResponse<List<RecurringExpenseResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllActive()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Yeni daimi ödəniş yarat")
    public ResponseEntity<ApiResponse<RecurringExpenseResponse>> create(
            @Valid @RequestBody RecurringExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Daimi ödəniş yaradıldı", service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Daimi ödənişi yenilə")
    public ResponseEntity<ApiResponse<RecurringExpenseResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RecurringExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Daimi ödəniş yeniləndi", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:DELETE')")
    @Operation(summary = "Daimi ödənişi sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Daimi ödəniş silindi"));
    }

    @PostMapping("/{id}/generate")
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Daimi ödənişdən qaimə yarat")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateInvoice(
            @PathVariable Long id, @Valid @RequestBody GenerateFromRecurringRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə yaradıldı", service.generateInvoice(id, request)));
    }
}
