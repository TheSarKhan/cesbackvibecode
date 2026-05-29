package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.hr.dto.*;
import com.ces.erp.hr.service.DeductionConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Generic tutulma (vergi/sığorta) konfiqurasiyası — növlər, versiyalar, aralıqlar, önizləmə.
 */
@RestController
@RequestMapping("/api/hr/deduction-config")
@RequiredArgsConstructor
@Tag(name = "HR — Tutulma Konfiqurasiyası", description = "Vergi və sığorta tutulmalarının konfiqurasiyası")
public class DeductionConfigController {

    private final DeductionConfigService service;

    // ── Tutulma növləri ──
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Bütün tutulma növlərini gətir")
    public ResponseEntity<ApiResponse<List<DeductionTypeDto>>> getTypes() {
        return ResponseEntity.ok(ApiResponse.success(service.getTypes()));
    }

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni tutulma növü əlavə et")
    public ResponseEntity<ApiResponse<DeductionTypeDto>> createType(@RequestBody DeductionTypeDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Tutulma növü yaradıldı", service.createType(dto)));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "Tutulma növünü yenilə")
    public ResponseEntity<ApiResponse<DeductionTypeDto>> updateType(@PathVariable Long id,
                                                                    @RequestBody DeductionTypeDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Tutulma növü yeniləndi", service.updateType(id, dto)));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    @Operation(summary = "Tutulma növünü sil")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long id) {
        service.deleteType(id);
        return ResponseEntity.ok(ApiResponse.ok("Tutulma növü silindi"));
    }

    // ── Versiyalar ──
    @GetMapping("/versions")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Bütün versiyaların siyahısı")
    public ResponseEntity<ApiResponse<List<DeductionConfigVersionResponse>>> getVersions() {
        return ResponseEntity.ok(ApiResponse.success(service.getVersions()));
    }

    @GetMapping("/versions/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Versiyanı aralıqları ilə gətir")
    public ResponseEntity<ApiResponse<DeductionConfigVersionResponse>> getVersion(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getVersion(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Aktiv versiyanı gətir")
    public ResponseEntity<ApiResponse<DeductionConfigVersionResponse>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getActive()));
    }

    @PostMapping("/versions")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni versiya yarat (dəyişiklik = yeni versiya)")
    public ResponseEntity<ApiResponse<DeductionConfigVersionResponse>> createVersion(
            @Valid @RequestBody CreateVersionRequest req, Authentication auth) {
        String user = auth != null ? auth.getName() : "system";
        return ResponseEntity.ok(ApiResponse.success("Versiya yaradıldı", service.createVersion(req, user)));
    }

    @PatchMapping("/versions/{id}/activate")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "Versiyanı aktivləşdir")
    public ResponseEntity<ApiResponse<DeductionConfigVersionResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Versiya aktivləşdirildi", service.activate(id)));
    }

    @DeleteMapping("/versions/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    @Operation(summary = "Versiyanı sil")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(@PathVariable Long id) {
        service.deleteVersion(id);
        return ResponseEntity.ok(ApiResponse.ok("Versiya silindi"));
    }

    // ── Canlı önizləmə ──
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Canlı önizləmə — yadda saxlanmamış aralıqlarla nümunə hesablama")
    public ResponseEntity<ApiResponse<DeductionPreviewResponse>> preview(@Valid @RequestBody DeductionPreviewRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.preview(req)));
    }
}
