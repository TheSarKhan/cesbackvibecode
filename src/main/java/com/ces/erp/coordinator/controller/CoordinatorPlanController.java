package com.ces.erp.coordinator.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.enums.RequestStatus;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coordinator")
@RequiredArgsConstructor
@Tag(name = "Coordinator", description = "Koordinator planlaması")
public class CoordinatorPlanController {

    private final CoordinatorPlanService planService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('COORDINATOR:GET')")
    @Operation(summary = "Koordinator status üzrə sorğu sayları (kartlar üçün)")
    public ResponseEntity<ApiResponse<Map<RequestStatus, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(planService.getStats()));
    }

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
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(planService.getRequestsPaged(page, size, q, status, sortBy, sortDir)));
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
    @Operation(summary = "Planı bitir və sorğuyu PM-ə qaytar (COORDINATOR_PROPOSED)")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> submitPlan(@PathVariable Long requestId) {
        planService.validateBeforeSubmit(requestId);
        return ResponseEntity.ok(ApiResponse.success("Təklif göndərildi",
                planService.submitPlan(requestId)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "[Köhnə endpoint — yeni flowda PM təsdiqləyir] Təklifi qəbul et")
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

    @PostMapping("/requests/{requestId}/withdraw-offer")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Təklifi geri al və yenidən danışığa qayıt (COORDINATOR_PROPOSED → COORDINATOR_NEGOTIATING, səbəb məcburi)")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> withdrawOffer(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Təklif geri alındı",
                planService.withdrawOffer(requestId, reason)));
    }

    // ─── Mərhələ B: İcra ─────────────────────────────────────────────────────

    @PostMapping("/requests/{requestId}/assign-operator")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Operator təyin et (EXECUTION_READY → OPERATOR_ASSIGNED)")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> assignOperator(
            @PathVariable Long requestId,
            @RequestParam Long operatorId) {
        return ResponseEntity.ok(ApiResponse.success("Operator təyin edildi",
                planService.assignOperator(requestId, operatorId)));
    }

    @PostMapping("/requests/{requestId}/reset-operator")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Operatoru dəyişmək üçün geri qaytar (OPERATOR_ASSIGNED → EXECUTION_READY, səbəb məcburi)")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> resetOperator(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Operator təyini sıfırlandı",
                planService.resetOperator(requestId, reason)));
    }

    @PostMapping("/requests/{requestId}/verify-equipment-docs")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Texnika sənədlərini yoxlanıldı kimi işarələ")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> verifyEquipmentDocs(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Texnika sənədləri yoxlanıldı",
                planService.verifyEquipmentDocs(requestId)));
    }

    @PutMapping("/requests/{requestId}/doc-check")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Yoxlama checklist-ində tək sənəd tipini işarələ / işarəni götür")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> toggleDocCheck(
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> body) {
        Long configItemId = body.get("configItemId") != null
                ? Long.valueOf(body.get("configItemId").toString()) : null;
        boolean checked = Boolean.TRUE.equals(body.get("checked"));
        return ResponseEntity.ok(ApiResponse.success("Yadda saxlandı",
                planService.toggleDocCheck(requestId, configItemId, checked)));
    }

    @PostMapping("/requests/{requestId}/dispatch")
    @PreAuthorize("hasAuthority('COORDINATOR:DISPATCH')")
    @Operation(summary = "Texnikanı yüklə və göndər (OPERATOR_ASSIGNED → EQUIPMENT_DISPATCHED)")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> dispatch(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Texnika göndərildi", planService.dispatch(requestId)));
    }

    @PostMapping("/requests/{requestId}/deliver")
    @PreAuthorize("hasAuthority('COORDINATOR:DELIVER')")
    @Operation(summary = "Təhvil-təslim tamamla (EQUIPMENT_DISPATCHED → DELIVERED + Project ACTIVE)")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> deliver(
            @PathVariable Long requestId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success("Təhvil-təslim tamamlandı",
                planService.deliver(requestId, notes)));
    }

    // ─── İcra: hər texnika xətti ayrı (çoxlu model) ──────────────────────────

    @PostMapping("/requests/{requestId}/items/{itemId}/assign-operator")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Xəttə operator təyin et")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> assignOperatorItem(
            @PathVariable Long requestId, @PathVariable Long itemId, @RequestParam Long operatorId) {
        return ResponseEntity.ok(ApiResponse.success("Operator təyin edildi",
                planService.assignOperatorItem(requestId, itemId, operatorId)));
    }

    @PostMapping("/requests/{requestId}/items/{itemId}/reset-operator")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Xəttin operatorunu sıfırla")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> resetOperatorItem(
            @PathVariable Long requestId, @PathVariable Long itemId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Operator sıfırlandı",
                planService.resetOperatorItem(requestId, itemId, reason)));
    }

    @PutMapping("/requests/{requestId}/items/{itemId}/doc-check")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Xəttin sənəd checklist-ində işarə qoy/götür")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> toggleDocCheckItem(
            @PathVariable Long requestId, @PathVariable Long itemId,
            @RequestBody Map<String, Object> body) {
        Long configItemId = body.get("configItemId") != null ? Long.valueOf(body.get("configItemId").toString()) : null;
        boolean checked = Boolean.TRUE.equals(body.get("checked"));
        return ResponseEntity.ok(ApiResponse.success("Yadda saxlandı",
                planService.toggleDocCheckItem(requestId, itemId, configItemId, checked)));
    }

    @PostMapping("/requests/{requestId}/items/{itemId}/verify-equipment-docs")
    @PreAuthorize("hasAuthority('COORDINATOR:PUT')")
    @Operation(summary = "Xəttin texnika sənədlərini yoxlanıldı kimi işarələ")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> verifyDocsItem(
            @PathVariable Long requestId, @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success("Sənədlər yoxlanıldı",
                planService.verifyDocsItem(requestId, itemId)));
    }

    @PostMapping("/requests/{requestId}/items/{itemId}/dispatch")
    @PreAuthorize("hasAuthority('COORDINATOR:DISPATCH')")
    @Operation(summary = "Xəttin texnikasını yüklə və göndər")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> dispatchItem(
            @PathVariable Long requestId, @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success("Texnika göndərildi",
                planService.dispatchItem(requestId, itemId)));
    }

    @PostMapping("/requests/{requestId}/items/{itemId}/deliver")
    @PreAuthorize("hasAuthority('COORDINATOR:DELIVER')")
    @Operation(summary = "Xəttin təhvil-təslimini tamamla")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> deliverItem(
            @PathVariable Long requestId, @PathVariable Long itemId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success("Təhvil-təslim tamamlandı",
                planService.deliverItem(requestId, itemId, notes)));
    }

    @PostMapping("/requests/{requestId}/items/{itemId}/documents")
    @PreAuthorize("hasAuthority('COORDINATOR:POST')")
    @Operation(summary = "Xəttə sənəd (təhvil-təslim aktı) yüklə")
    public ResponseEntity<ApiResponse<CoordinatorPlanResponse>> uploadItemDocument(
            @PathVariable Long requestId, @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "HANDOVER_ACT") String documentType,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd əlavə edildi",
                planService.uploadItemDocument(requestId, itemId, file, documentType, principal.getId())));
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
            @PathVariable Long documentId) throws MalformedURLException, IOException {
        Path filePath = planService.resolveDocument(requestId, documentId);
        Resource resource = new UrlResource(filePath.toUri());
        String ct = Files.probeContentType(filePath);
        MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/requests/{requestId}/equipment-documents/{documentId}/download")
    @PreAuthorize("hasAuthority('COORDINATOR:GET')")
    @Operation(summary = "Texnikanın sənədini yüklə (koordinator yoxlaması üçün)")
    public ResponseEntity<Resource> downloadEquipmentDocument(
            @PathVariable Long requestId,
            @PathVariable Long documentId) throws MalformedURLException, IOException {
        Path filePath = planService.resolveEquipmentDocument(requestId, documentId);
        Resource resource = new UrlResource(filePath.toUri());
        String ct = Files.probeContentType(filePath);
        MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}
