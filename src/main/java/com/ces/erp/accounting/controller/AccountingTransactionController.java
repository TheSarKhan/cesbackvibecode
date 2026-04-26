package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.TransactionRequest;
import com.ces.erp.accounting.dto.TransactionResponse;
import com.ces.erp.accounting.service.TransactionService;
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
@RequestMapping("/api/accounting/transactions")
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Mühasibatlıq — Tranzaksiyalar")
public class AccountingTransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Bütün tranzaksiyaları gətir")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Tranzaksiyanı ID ilə gətir")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Yeni tranzaksiya yarat")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Əməliyyat qeydə alındı", transactionService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Tranzaksiyanı yenilə")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Əməliyyat yeniləndi", transactionService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:DELETE')")
    @Operation(summary = "Tranzaksiyanı sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Əməliyyat silindi"));
    }
}
