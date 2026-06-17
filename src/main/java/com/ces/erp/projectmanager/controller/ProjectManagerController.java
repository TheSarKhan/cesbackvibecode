package com.ces.erp.projectmanager.controller;

import com.ces.erp.accounting.service.DocumentCheckService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.projectmanager.dto.CustomerAgreementRequest;
import com.ces.erp.projectmanager.dto.CustomerContactRequest;
import com.ces.erp.projectmanager.dto.PmRequestResponse;
import com.ces.erp.projectmanager.dto.ShortlistSaveRequest;
import com.ces.erp.projectmanager.service.ProjectManagerService;
import com.ces.erp.request.entity.RequestDocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;

@RestController
@RequestMapping("/api/project-manager")
@RequiredArgsConstructor
@Tag(name = "Project Manager", description = "Layihə Meneceri əməliyyatları (shortlist, qiymət razılaşması, təsdiq)")
public class ProjectManagerController {

    private final ProjectManagerService service;
    private final DocumentCheckService documentService;

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "PM-in görəcəyi sorğular (filter olunmamış)")
    public ResponseEntity<ApiResponse<List<PmRequestResponse>>> getRequests() {
        return ResponseEntity.ok(ApiResponse.success(service.getRequests()));
    }

    @GetMapping("/requests/paged")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "Sorğular - səhifələnmiş")
    public ResponseEntity<ApiResponse<PagedResponse<PmRequestResponse>>> getRequestsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getRequestsPaged(page, size, search, status, sortBy, sortDir)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "PM-də status üzrə sorğu sayları (kartlar üçün)")
    public ResponseEntity<ApiResponse<Map<RequestStatus, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(service.getStats()));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "Sorğu detalı (shortlist + koordinator təklifi ilə)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> getRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(service.getRequest(requestId)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Sorğunu qəbul et (PENDING → PM_REVIEW)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> accept(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu qəbul edildi", service.accept(requestId)));
    }

    @PostMapping("/requests/{requestId}/shortlist")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:POST')")
    @Operation(summary = "Shortlist sətirlərini saxla (upsert)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveShortlist(
            @PathVariable Long requestId,
            @RequestBody ShortlistSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shortlist yadda saxlandı",
                service.saveShortlist(requestId, request)));
    }

    @DeleteMapping("/requests/{requestId}/shortlist/items/{itemId}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:DELETE')")
    @Operation(summary = "Shortlist sətirini sil")
    public ResponseEntity<ApiResponse<Void>> deleteShortlistItem(
            @PathVariable Long requestId, @PathVariable Long itemId) {
        service.deleteShortlistItem(requestId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Sətir silindi"));
    }

    @PostMapping("/requests/{requestId}/send-to-coordinator")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Shortlisti koordinatora göndər (status → COORDINATOR_NEGOTIATING)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> sendToCoordinator(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Koordinatora göndərildi",
                service.sendToCoordinator(requestId)));
    }

    @PutMapping("/requests/{requestId}/customer-agreement")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Sifarişçi ilə razılaşdırılmış qiyməti və qeydi saxla")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveCustomerAgreement(
            @PathVariable Long requestId,
            @RequestBody CustomerAgreementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Razılaşma qeyd edildi",
                service.saveCustomerAgreement(requestId, request)));
    }

    @PutMapping("/requests/{requestId}/items/{itemId}/customer-agreement")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Çoxlu texnika — bir xətt üçün sifarişçi razılaşmasını saxla")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveCustomerAgreementItem(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestBody CustomerAgreementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Razılaşma qeyd edildi",
                service.saveCustomerAgreementItem(requestId, itemId, request)));
    }

    @PutMapping("/requests/{requestId}/customer-contact")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "LM 1.3 — Sifarişçi ofisindəki əlaqə şəxsini qeyd et")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveCustomerContact(
            @PathVariable Long requestId,
            @RequestBody CustomerContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Əlaqə məlumatı saxlandı",
                service.saveCustomerContact(requestId, request)));
    }

    @PutMapping("/requests/{requestId}/required-documents")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "LM — texnika üçün tələb olunan əlavə sənədləri dəqiqləşdir")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveRequiredDocuments(
            @PathVariable Long requestId,
            @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        java.util.List<Long> ids = body != null ? body.get("documentItemIds") : null;
        return ResponseEntity.ok(ApiResponse.success("Tələb olunan sənədlər saxlandı",
                service.saveRequiredDocuments(requestId, ids)));
    }

    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:APPROVE_PM')")
    @Operation(summary = "Sorğunu təsdiqlə (Layihə yaradılır, mühasibatlığa göndərilir)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> approve(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu təsdiqləndi", service.approve(requestId)));
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Sorğunu rədd et")
    public ResponseEntity<ApiResponse<PmRequestResponse>> reject(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Sorğu rədd edildi", service.reject(requestId, reason)));
    }

    @PostMapping("/requests/{requestId}/send-back-to-coordinator")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Koordinatora geri qaytar (PM_PRICE_NEGOTIATION → COORDINATOR_NEGOTIATING, səbəb məcburi)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> sendBackToCoordinator(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Koordinatora geri qaytarıldı",
                service.sendBackToCoordinator(requestId, reason)));
    }

    // ─── Sənəd yükləmə (PM_PRICE_NEGOTIATION mərhələsində) ────────────────────
    // PM müştəri ilə razılaşmadan sonra müqavilə və qiymət protokolunu burada
    // yükləyə bilir. Eyni sənədlər sonradan mühasibatlıqda da görünür.

    @PostMapping(value = "/requests/{requestId}/upload-contract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:POST')")
    @Operation(summary = "Müqavilə faylı yüklə (PM tərəfindən)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> uploadContract(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.uploadDocument(requestId, RequestDocumentType.CONTRACT, file, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Müqavilə yükləndi", service.getRequest(requestId)));
    }

    @PostMapping(value = "/requests/{requestId}/upload-price-protocol",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:POST')")
    @Operation(summary = "Qiymət razılaşma protokolu yüklə (PM tərəfindən)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> uploadPriceProtocol(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.uploadDocument(requestId, RequestDocumentType.PRICE_PROTOCOL, file, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Protokol yükləndi", service.getRequest(requestId)));
    }

    // ─── Çoxlu texnika: xətt üzrə sənəd yükləmə ───────────────────────────────

    @PostMapping(value = "/requests/{requestId}/items/{itemId}/upload-contract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:POST')")
    @Operation(summary = "Bir texnika xətti üçün müqavilə faylı yüklə")
    public ResponseEntity<ApiResponse<PmRequestResponse>> uploadContractItem(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.uploadDocument(requestId, RequestDocumentType.CONTRACT, file, principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Müqavilə yükləndi", service.getRequest(requestId)));
    }

    @PostMapping(value = "/requests/{requestId}/items/{itemId}/upload-price-protocol",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:POST')")
    @Operation(summary = "Bir texnika xətti üçün qiymət razılaşma protokolu yüklə")
    public ResponseEntity<ApiResponse<PmRequestResponse>> uploadPriceProtocolItem(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.uploadDocument(requestId, RequestDocumentType.PRICE_PROTOCOL, file, principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Protokol yükləndi", service.getRequest(requestId)));
    }

    @DeleteMapping("/requests/{requestId}/documents/{documentId}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:DELETE')")
    @Operation(summary = "Yüklənmiş sənədi sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long requestId, @PathVariable Long documentId) {
        documentService.deleteDocument(requestId, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/requests/{requestId}/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "Sənədi yüklə")
    public ResponseEntity<UrlResource> downloadDocument(
            @PathVariable Long requestId, @PathVariable Long documentId) throws MalformedURLException {
        Path path = documentService.resolveDocumentFile(requestId, documentId);
        UrlResource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }
}
