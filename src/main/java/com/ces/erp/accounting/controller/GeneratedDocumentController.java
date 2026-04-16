package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.*;
import com.ces.erp.accounting.service.GeneratedDocumentService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Sənəd Generasiya Modulu — Hesab-Faktura, Akt, Invoice")
public class GeneratedDocumentController {

    private final GeneratedDocumentService documentService;

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Sənədləri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<GeneratedDocumentResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "15") int    size,
            @RequestParam(required = false)    String q,
            @RequestParam(required = false)    String type) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getAllPaged(page, size, q, type)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Sənədi ID ilə gətir")
    public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Yeni sənəd yarat (PDF avtomatik generasiya edilir)")
    public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> create(
            @Valid @RequestBody GeneratedDocumentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yaradıldı", documentService.create(req)));
    }

    @PostMapping("/preview-lines")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Təsdiqlənmiş qaimələrdən sənəd sətirləri hazırla")
    public ResponseEntity<ApiResponse<List<DocumentLineRequest>>> previewLines(
            @RequestBody DocumentPreviewRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.previewFromInvoices(req.getInvoiceIds())));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "PDF yüklə")
    public ResponseEntity<UrlResource> downloadPdf(@PathVariable Long id) {
        GeneratedDocumentResponse doc = documentService.getById(id);
        UrlResource resource = documentService.downloadPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "PDF-i yenidən yarat")
    public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> regeneratePdf(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("PDF yenidən yaradıldı", documentService.regeneratePdf(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:DELETE')")
    @Operation(summary = "Sənədi sil (soft delete + PDF fayl silinir)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }
}
