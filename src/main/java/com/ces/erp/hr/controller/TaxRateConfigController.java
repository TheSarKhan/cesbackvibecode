package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.hr.dto.TaxRateConfigRequest;
import com.ces.erp.hr.dto.TaxRateConfigResponse;
import com.ces.erp.hr.service.TaxRateConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/tax-rates")
@RequiredArgsConstructor
@Tag(name = "HR — Vergi Tarifləri", description = "İllik vergi və sığorta tarifləri konfiqurasiyası")
public class TaxRateConfigController {

    private final TaxRateConfigService service;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Bütün illik tarifləri gətir")
    public ResponseEntity<ApiResponse<List<TaxRateConfigResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Aktiv tarifi gətir")
    public ResponseEntity<ApiResponse<TaxRateConfigResponse>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<TaxRateConfigResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni illik tarif yarat")
    public ResponseEntity<ApiResponse<TaxRateConfigResponse>> create(@Valid @RequestBody TaxRateConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Tarif yaradıldı", service.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "Tarifi yenilə")
    public ResponseEntity<ApiResponse<TaxRateConfigResponse>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody TaxRateConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Tarif yeniləndi", service.update(id, req)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "Tarifi aktivləşdir")
    public ResponseEntity<ApiResponse<TaxRateConfigResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tarif aktivləşdirildi", service.activate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tarif silindi"));
    }
}
