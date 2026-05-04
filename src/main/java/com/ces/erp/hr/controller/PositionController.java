package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.hr.dto.PositionRequest;
import com.ces.erp.hr.dto.PositionResponse;
import com.ces.erp.hr.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/positions")
@RequiredArgsConstructor
@Tag(name = "HR — Vəzifələr", description = "İnsan Resursları: Vəzifə kataloqu")
public class PositionController {

    private final PositionService positionService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Bütün vəzifələri gətir")
    public ResponseEntity<ApiResponse<List<PositionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(positionService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Vəzifəni ID ilə gətir")
    public ResponseEntity<ApiResponse<PositionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(positionService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni vəzifə yarat")
    public ResponseEntity<ApiResponse<PositionResponse>> create(@Valid @RequestBody PositionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Vəzifə yaradıldı", positionService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "Vəzifəni yenilə")
    public ResponseEntity<ApiResponse<PositionResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody PositionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Vəzifə yeniləndi", positionService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    @Operation(summary = "Vəzifəni sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Vəzifə silindi"));
    }
}
