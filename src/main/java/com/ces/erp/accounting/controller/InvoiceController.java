package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.AccountingSummaryResponse;
import com.ces.erp.accounting.dto.InvoiceFieldsRequest;
import com.ces.erp.accounting.dto.InvoiceRequest;
import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.service.InvoiceService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.enums.InvoiceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Mühasibatlıq — E-Qaimə modulu")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Bütün qaimələri gətir")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAll(
            @RequestParam(required = false) InvoiceType type) {
        List<InvoiceResponse> result = type != null
                ? invoiceService.getByType(type)
                : invoiceService.getAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Qaimələri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAllPaged(page, size, q, type)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Maliyyə xülasəsi — ümumi gəlir, xərc, xalis mənfəət")
    public ResponseEntity<ApiResponse<AccountingSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getSummary()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Qaiməni ID ilə gətir")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @GetMapping("/by-project/{projectId}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Layihəyə aid bütün qaimələri gətir")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByProjectId(projectId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Yeni qaimə yarat")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody InvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə əlavə edildi", invoiceService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Qaiməni yenilə")
    public ResponseEntity<ApiResponse<InvoiceResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə yeniləndi", invoiceService.update(id, req)));
    }

    @PatchMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Qaimənin inzibati sahələrini doldur (ETaxes ID, nömrə, tarix, qeyd)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> patchFields(
            @PathVariable Long id,
            @RequestBody InvoiceFieldsRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Sahələr yeniləndi", invoiceService.patchFields(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:DELETE')")
    @Operation(summary = "Qaiməni sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Qaimə silindi"));
    }
}
