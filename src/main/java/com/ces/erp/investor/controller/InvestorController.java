package com.ces.erp.investor.controller;

import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.coordinator.dto.ProjectHistoryItem;
import com.ces.erp.investor.dto.InvestorPortalAccountRequest;
import com.ces.erp.investor.dto.InvestorRequest;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.dto.InvestorSetPasswordRequest;
import com.ces.erp.investor.service.InvestorService;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.partydoc.PartyDocumentDto;
import com.ces.erp.partydoc.PartyDocumentService;
import com.ces.erp.partydoc.PartyKind;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investors")
@RequiredArgsConstructor
@Tag(name = "Investors", description = "İnvestor idarəetməsi")
public class InvestorController {

    private final InvestorService investorService;
    private final PartyDocumentService partyDocumentService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "Bütün investorları gətir")
    public ResponseEntity<ApiResponse<List<InvestorResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(investorService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "İnvestorları səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<InvestorResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String risk) {
        return ResponseEntity.ok(ApiResponse.success(investorService.getAllPaged(page, size, q, status, risk)));
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

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAuthority('INVESTORS:DELETE')")
    @Operation(summary = "Seçilmiş investorları sil")
    public ResponseEntity<ApiResponse<Void>> deleteAll(@RequestBody Map<String, List<Long>> body) {
        investorService.deleteAll(body.get("ids"));
        return ResponseEntity.ok(ApiResponse.ok("İnvestorlar silindi"));
    }

    @GetMapping("/{id}/projects")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "İnvestorun layihə tarixçəsi")
    public ResponseEntity<ApiResponse<List<ProjectHistoryItem>>> getProjectHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(investorService.getProjectHistory(id)));
    }

    @GetMapping("/{id}/invoices")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "İnvestorun qaimələri")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(investorService.getInvoices(id)));
    }

    @GetMapping("/{id}/payables")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "İnvestora edilmiş ödənişlər")
    public ResponseEntity<ApiResponse<List<PayableResponse>>> getPayables(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(investorService.getPayables(id)));
    }

    // ─── Sənəd mərkəzi (bütün mənbələrdən aqreqasiya) ────────────────────────

    @GetMapping("/{id}/all-documents")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "İnvestorun BÜTÜN sənədləri (əl ilə + müqavilə + akt + texnika + qaimə)")
    public ResponseEntity<ApiResponse<List<PartyDocumentDto>>> allDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                partyDocumentService.collect(PartyKind.INVESTOR, id)));
    }

    @PostMapping(value = "/{id}/all-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVESTORS:POST')")
    @Operation(summary = "İnvestora əl ilə sənəd yüklə")
    public ResponseEntity<ApiResponse<PartyDocumentDto>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "documentDate", required = false) String documentDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yükləndi",
                partyDocumentService.uploadManual(PartyKind.INVESTOR, id, file, documentName, documentDate,
                        principal != null ? principal.getId() : null)));
    }

    @DeleteMapping("/{id}/all-documents/{documentId}")
    @PreAuthorize("hasAuthority('INVESTORS:DELETE')")
    @Operation(summary = "Əl ilə yüklənmiş sənədi sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        partyDocumentService.deleteManual(PartyKind.INVESTOR, id, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/{id}/all-documents/{sourceType}/{sourceId}/download")
    @PreAuthorize("hasAuthority('INVESTORS:GET')")
    @Operation(summary = "Sənəd mərkəzindən sənəd endir")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id,
                                                     @PathVariable String sourceType,
                                                     @PathVariable Long sourceId) throws IOException {
        var df = partyDocumentService.resolveDownload(PartyKind.INVESTOR, id, sourceType, sourceId);
        Path path = df.path();
        Resource resource = new UrlResource(path.toUri());
        String ct = Files.probeContentType(path);
        MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;
        String fileName = df.fileName() != null ? df.fileName() : path.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    // ─── Portal hesab idarəsi (admin) ─────────────────────────────────────────

    @PutMapping("/{id}/portal-account")
    @PreAuthorize("hasAuthority('INVESTORS:PUT')")
    @Operation(summary = "Portal hesab maili + aktiv/passiv təyini")
    public ResponseEntity<ApiResponse<InvestorResponse>> updatePortalAccount(
            @PathVariable Long id, @Valid @RequestBody InvestorPortalAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Portal hesabı yeniləndi",
                investorService.updatePortalAccount(id, request)));
    }

    @PostMapping("/{id}/set-password")
    @PreAuthorize("hasAuthority('INVESTORS:PUT')")
    @Operation(summary = "Portal şifrəsini təyin et / sıfırla (admin)")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @PathVariable Long id, @Valid @RequestBody InvestorSetPasswordRequest request) {
        investorService.setPassword(id, request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok("Şifrə təyin edildi"));
    }
}
