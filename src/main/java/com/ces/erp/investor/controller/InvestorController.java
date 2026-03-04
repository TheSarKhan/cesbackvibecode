package com.ces.erp.investor.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.investor.dto.InvestorRequest;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.service.InvestorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investors")
@RequiredArgsConstructor
@Tag(name = "Investors", description = "İnvestor idarəetməsi")
public class InvestorController {

    private final InvestorService investorService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "Bütün investorları gətir")
    public ResponseEntity<ApiResponse<List<InvestorResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(investorService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "İnvestoru ID ilə gətir")
    public ResponseEntity<ApiResponse<InvestorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(investorService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVESTORS:POST')")
    @Operation(summary = "Yeni investor əlavə et")
    public ResponseEntity<ApiResponse<InvestorResponse>> create(@Valid @RequestBody InvestorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("İnvestor əlavə edildi", investorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVESTORS:PUT')")
    @Operation(summary = "İnvestoru yenilə")
    public ResponseEntity<ApiResponse<InvestorResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody InvestorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("İnvestor yeniləndi", investorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVESTORS:DELETE')")
    @Operation(summary = "İnvestoru sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        investorService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("İnvestor silindi"));
    }
}
