package com.ces.erp.coordinator.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.coordinator.dto.CoordinatorPlanRequest;
import com.ces.erp.coordinator.dto.CoordinatorPlanResponse;
import com.ces.erp.coordinator.service.CoordinatorPlanService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/coordinator")
@RequiredArgsConstructor
@Tag(name = "Coordinator", description = "Koordinator planlaması")
public class CoordinatorPlanController {

    private final CoordinatorPlanService planService;

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('COORDINATOR:GET')")
    @Operation(summary = "Koordinatora gələn sorğuları gətir")
    public ResponseEntity<ApiResponse<List<CoordinatorPlanResponse>>> getRequests() {
        return ResponseEntity.ok(ApiResponse.success(planService.getRequests()));
    }

    @GetMapping("/requests/paged")
    @PreAuthorize("hasAuthority('COORDINATOR:GET')")
    @Operation(summary = "Koordinator sorğularını səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<CoordinatorPlanResponse>>> getRequestsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(planService.getRequestsPaged(page, size, q, status)));
    }

    @GetMapping("/requests/{requestId}/plan")
    @PreAuthorize("hasAuthority('COORDINATOR:GET')")
    @Operation(summary = "Sorğunun koordinator planını gətir")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> getPlan(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlan(requestId)));
    }

    @PostMapping("/requests/{requestId}/plan")
    @PreAuthorize("hasAuthority('COORDINATOR:POST')")
    @Operation(summary = "Koordinator planını yarat və ya yenilə")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> savePlan(
            @PathVariable Long requestId,
            @Valid @RequestBody CoordinatorPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Plan yadda saxlandı",
                planService.savePlan(requestId, request, principal.getId())));
    }

    @PostMapping("/requests/{requestId}/submit")
    @PreAuthorize("hasAuthority('COORDINATOR:SUBMIT_OFFER')")
    @Operation(summary = "Planı bitir və sorğuyu OFFER_SENT statusuna keçir")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> submitPlan(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Təklif göndərildi",
                planService.submitPlan(requestId)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Təklifi qəbul et — ACCEPTED statusuna keçir, Layihə yaradılır")
    public ResponseEntity<ApiResponse<Void>> acceptOffer(@PathVariable Long requestId) {
        planService.acceptOffer(requestId);
        return ResponseEntity.ok(ApiResponse.ok("Təklif qəbul edildi, layihə yaradıldı"));
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Təklifi rədd et — REJECTED statusuna keçir")
    public ResponseEntity<ApiResponse<Void>> rejectOffer(@PathVariable Long requestId) {
        planService.rejectOffer(requestId);
        return ResponseEntity.ok(ApiResponse.ok("Təklif rədd edildi"));
    }

    @PutMapping("/requests/{requestId}/equipment")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Koordinator sorğu üçün texnika seçir")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> selectEquipment(
            @PathVariable Long requestId,
            @RequestParam Long equipmentId) {
        return ResponseEntity.ok(ApiResponse.success("Texnika seçildi",
                planService.selectEquipment(requestId, equipmentId)));
    }

    @PostMapping("/requests/{requestId}/plan/documents")
    @PreAuthorize("hasAuthority('COORDINATOR:POST')")
    @Operation(summary = "Koordinator planına sənəd əlavə et")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse.DocumentDto>> uploadDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "documentType", defaultValue = "OTHER") String documentType,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd əlavə edildi",
                planService.uploadDocument(requestId, file, documentName, documentType, principal.getId())));
    }

    @DeleteMapping("/requests/{requestId}/plan/documents/{documentId}")
    @PreAuthorize("hasAuthority('COORDINATOR:DELETE')")
    @Operation(summary = "Koordinator sənədini sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long requestId,
            @PathVariable Long documentId) {
        planService.deleteDocument(requestId, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/requests/{requestId}/plan/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('COORDINATOR:GET')")
    @Operation(summary = "Koordinator sənədini yüklə")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long requestId,
            @PathVariable Long documentId) throws MalformedURLException {
        Path filePath = planService.resolveDocument(requestId, documentId);
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
