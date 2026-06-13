package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.RequestDocumentCheckResponse;
import com.ces.erp.accounting.service.DocumentCheckService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.UserPrincipal;
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

@RestController
@RequestMapping("/api/accounting/document-checks")
@RequiredArgsConstructor
@Tag(name = "Mühasibatlıq Sənəd Yoxlanışı", description = "Sorğu müqaviləsi və qiymət protokolu")
public class DocumentCheckController {

    private final DocumentCheckService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Yoxlama gözləyən sorğuların siyahısı")
    public ResponseEntity<ApiResponse<List<RequestDocumentCheckResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(service.getPendingChecks()));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Sorğu üçün sənəd vəziyyəti")
    public ResponseEntity<ApiResponse<RequestDocumentCheckResponse>> get(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(service.getCheck(requestId)));
    }

    @PostMapping(value = "/{requestId}/upload-contract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Müqavilə faylı yüklə")
    public ResponseEntity<ApiResponse<RequestDocumentCheckResponse>> uploadContract(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Müqavilə yükləndi",
                service.uploadDocument(requestId, RequestDocumentType.CONTRACT, file, principal.getId())));
    }

    @PostMapping(value = "/{requestId}/upload-price-protocol", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Qiymət razılaşma protokolu yüklə")
    public ResponseEntity<ApiResponse<RequestDocumentCheckResponse>> uploadProtocol(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Protokol yükləndi",
                service.uploadDocument(requestId, RequestDocumentType.PRICE_PROTOCOL, file, principal.getId())));
    }

    @DeleteMapping("/{requestId}/documents/{documentId}")
    @PreAuthorize("hasAuthority('ACCOUNTING:DELETE')")
    @Operation(summary = "Yüklənmiş sənədi sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long requestId, @PathVariable Long documentId) {
        service.deleteDocument(requestId, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/{requestId}/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Sənədi yüklə")
    public ResponseEntity<UrlResource> downloadDocument(
            @PathVariable Long requestId, @PathVariable Long documentId) throws MalformedURLException {
        Path path = service.resolveDocumentFile(requestId, documentId);
        UrlResource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }

    @PostMapping("/{requestId}/complete-check")
    @PreAuthorize("hasAuthority('ACCOUNTING:CHECK_DOCUMENTS')")
    @Operation(summary = "Sənəd yoxlamasını OK ver — Əməliyyatların təsdiqinə göndərir (təsdiqdə layihə ACTIVE)")
    public ResponseEntity<ApiResponse<RequestDocumentCheckResponse>> completeCheck(@PathVariable Long requestId) {
        // Əvvəlcə validasiya (status + məcburi sənədlər) — submit anında dərhal xəta versin.
        service.assertReadyForActivation(requestId);
        // Sonra təsdiq qapısı — proxy sərhədini keçmək üçün BİRBAŞA annotasiyalı metod çağırılır.
        // Aspect əməliyyatı sıraya salıb PendingApprovalException (202) atır.
        return ResponseEntity.ok(ApiResponse.success("Təsdiqə göndərildi",
                service.submitForActivation(requestId)));
    }

    @PostMapping("/{requestId}/send-back")
    @PreAuthorize("hasAuthority('ACCOUNTING:CHECK_DOCUMENTS')")
    @Operation(summary = "LM-ə geri qaytar (ACCOUNTING_DOCS_CHECK → PM_PRICE_NEGOTIATION, səbəb məcburi)")
    public ResponseEntity<ApiResponse<RequestDocumentCheckResponse>> sendBack(
            @PathVariable Long requestId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Sorğu LM-ə geri qaytarıldı",
                service.sendBackToProjectManager(requestId, reason)));
    }
}
