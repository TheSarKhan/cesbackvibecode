package com.ces.erp.project.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.project.dto.FinanceEntryRequest;
import com.ces.erp.project.dto.ProjectCompleteRequest;
import com.ces.erp.project.dto.ProjectResponse;
import com.ces.erp.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Layihələr modulu")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECTS:GET')")
    @Operation(summary = "Bütün layihələri gətir")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('PROJECTS:GET')")
    @Operation(summary = "Layihələri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllPaged(page, size, q, status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECTS:GET')")
    @Operation(summary = "Layihəni ID ilə gətir")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getById(id)));
    }

    // ─── Müqavilə ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/contract")
    @PreAuthorize("hasAuthority('PROJECTS:POST')")
    @Operation(summary = "Müqavilə sənədini yüklə — layihəni ACTIVE edir")
    public ResponseEntity<ApiResponse<ProjectResponse>> uploadContract(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "startDate", required = false) String startDateStr) {
        LocalDate startDate = startDateStr != null && !startDateStr.isBlank()
                ? LocalDate.parse(startDateStr) : null;
        return ResponseEntity.ok(ApiResponse.success("Müqavilə yükləndi. Layihə aktiv oldu.",
                projectService.uploadContract(id, file, startDate)));
    }

    @GetMapping("/{id}/contract")
    @PreAuthorize("hasAuthority('PROJECTS:GET')")
    @Operation(summary = "Müqavilə sənədini endir")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long id) throws MalformedURLException {
        Path filePath = projectService.resolveContract(id);
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // ─── Maliyyə — Xərclər ────────────────────────────────────────────────────

    @GetMapping("/{id}/finances")
    @PreAuthorize("hasAuthority('PROJECTS:GET')")
    @Operation(summary = "Layihənin xərc və gəlirlərini gətir")
    public ResponseEntity<ApiResponse<ProjectResponse.FinancesDto>> getFinances(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getFinances(id)));
    }

    @PostMapping("/{id}/expenses")
    @PreAuthorize("hasAuthority('PROJECTS:POST')")
    @Operation(summary = "Yeni xərc əlavə et")
    public ResponseEntity<ApiResponse<ProjectResponse.FinanceEntryDto>> addExpense(
            @PathVariable Long id,
            @Valid @RequestBody FinanceEntryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Xərc əlavə edildi",
                projectService.addExpense(id, req)));
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    @PreAuthorize("hasAuthority('PROJECTS:DELETE')")
    @Operation(summary = "Xərci sil")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long id,
            @PathVariable Long expenseId) {
        projectService.deleteExpense(id, expenseId);
        return ResponseEntity.ok(ApiResponse.ok("Xərc silindi"));
    }

    // ─── Maliyyə — Gəlirlər ───────────────────────────────────────────────────

    @PostMapping("/{id}/revenues")
    @PreAuthorize("hasAuthority('PROJECTS:POST')")
    @Operation(summary = "Yeni gəlir əlavə et")
    public ResponseEntity<ApiResponse<ProjectResponse.FinanceEntryDto>> addRevenue(
            @PathVariable Long id,
            @Valid @RequestBody FinanceEntryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Gəlir əlavə edildi",
                projectService.addRevenue(id, req)));
    }

    @DeleteMapping("/{id}/revenues/{revenueId}")
    @PreAuthorize("hasAuthority('PROJECTS:DELETE')")
    @Operation(summary = "Gəliri sil")
    public ResponseEntity<ApiResponse<Void>> deleteRevenue(
            @PathVariable Long id,
            @PathVariable Long revenueId) {
        projectService.deleteRevenue(id, revenueId);
        return ResponseEntity.ok(ApiResponse.ok("Gəlir silindi"));
    }

    // ─── Bağlanış ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PROJECTS:PUT')")
    @Operation(summary = "Layihəni bitir — COMPLETED statusuna keçirir")
    public ResponseEntity<ApiResponse<ProjectResponse>> complete(
            @PathVariable Long id,
            @Valid @RequestBody ProjectCompleteRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Layihə bağlandı. Mühasibatlığa yönləndirildi.",
                projectService.complete(id, req)));
    }

    // ─── Bitmə tarixi ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/end-date")
    @PreAuthorize("hasAuthority('PROJECTS:PUT')")
    @Operation(summary = "Layihənin bitmə tarixini yenilə")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateEndDate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        LocalDate endDate = LocalDate.parse(body.get("endDate"));
        return ResponseEntity.ok(ApiResponse.success("Bitmə tarixi yeniləndi",
                projectService.updateEndDate(id, endDate)));
    }

    // ─── Başlanğıc tarixi ─────────────────────────────────────────────────────

    @PatchMapping("/{id}/start-date")
    @PreAuthorize("hasAuthority('PROJECTS:PUT')")
    @Operation(summary = "Layihənin başlanğıc tarixini yenilə")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateStartDate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        return ResponseEntity.ok(ApiResponse.success("Başlanğıc tarixi yeniləndi",
                projectService.updateStartDate(id, startDate)));
    }
}
