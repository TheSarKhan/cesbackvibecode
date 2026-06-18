package com.ces.erp.contractor.controller;

import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.contractor.dto.ContractorRequest;
import com.ces.erp.contractor.dto.ContractorResponse;
import com.ces.erp.contractor.service.ContractorService;
import com.ces.erp.coordinator.dto.ProjectHistoryItem;
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

@RestController
@RequestMapping("/api/contractors")
@RequiredArgsConstructor
@Tag(name = "Contractors", description = "Podratçı idarəetməsi")
public class ContractorController {

    private final ContractorService contractorService;
    private final PartyDocumentService partyDocumentService;

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

    @GetMapping("/{id}/projects")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Podratçının layihə tarixçəsi")
    public ResponseEntity<ApiResponse<List<ProjectHistoryItem>>> getProjectHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contractorService.getProjectHistory(id)));
    }

    @GetMapping("/{id}/invoices")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Podratçının qaimələri")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contractorService.getInvoices(id)));
    }

    @GetMapping("/{id}/payables")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Podratçıya edilmiş ödənişlər")
    public ResponseEntity<ApiResponse<List<PayableResponse>>> getPayables(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contractorService.getPayables(id)));
    }

    // ─── Sənəd mərkəzi (bütün mənbələrdən aqreqasiya) ────────────────────────

    @GetMapping("/{id}/all-documents")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Podratçının BÜTÜN sənədləri (əl ilə + müqavilə + akt + texnika + qaimə)")
    public ResponseEntity<ApiResponse<List<PartyDocumentDto>>> allDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                partyDocumentService.collect(PartyKind.CONTRACTOR, id)));
    }

    @PostMapping(value = "/{id}/all-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:POST')")
    @Operation(summary = "Podratçıya əl ilə sənəd yüklə")
    public ResponseEntity<ApiResponse<PartyDocumentDto>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "documentDate", required = false) String documentDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yükləndi",
                partyDocumentService.uploadManual(PartyKind.CONTRACTOR, id, file, documentName, documentDate,
                        principal != null ? principal.getId() : null)));
    }

    @DeleteMapping("/{id}/all-documents/{documentId}")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:DELETE')")
    @Operation(summary = "Əl ilə yüklənmiş sənədi sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        partyDocumentService.deleteManual(PartyKind.CONTRACTOR, id, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/{id}/all-documents/{sourceType}/{sourceId}/download")
    @PreAuthorize("hasAuthority('CONTRACTOR_MANAGEMENT:GET')")
    @Operation(summary = "Sənəd mərkəzindən sənəd endir")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id,
                                                     @PathVariable String sourceType,
                                                     @PathVariable Long sourceId) throws IOException {
        var df = partyDocumentService.resolveDownload(PartyKind.CONTRACTOR, id, sourceType, sourceId);
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
}
