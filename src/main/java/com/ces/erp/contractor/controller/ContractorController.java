package com.ces.erp.contractor.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.contractor.dto.ContractorRequest;
import com.ces.erp.contractor.dto.ContractorResponse;
import com.ces.erp.contractor.service.ContractorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contractors")
@RequiredArgsConstructor
@Tag(name = "Contractors", description = "Podratçı idarəetməsi")
public class ContractorController {

    private final ContractorService contractorService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Bütün podratçıları gətir")
    public ResponseEntity<ApiResponse<List<ContractorResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(contractorService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Podratçıları səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<ContractorResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String risk) {
        return ResponseEntity.ok(ApiResponse.success(contractorService.getAllPaged(page, size, q, status, risk)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Podratçını ID ilə gətir")
    public ResponseEntity<ApiResponse<ContractorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contractorService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni podratçı əlavə et")
    public ResponseEntity<ApiResponse<ContractorResponse>> create(@Valid @RequestBody ContractorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Podratçı əlavə edildi", contractorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:PUT')")
    @Operation(summary = "Podratçını yenilə")
    public ResponseEntity<ApiResponse<ContractorResponse>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ContractorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Podratçı yeniləndi", contractorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:DELETE')")
    @Operation(summary = "Podratçını sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        contractorService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Podratçı silindi"));
    }
}
