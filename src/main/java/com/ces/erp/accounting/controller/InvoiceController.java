package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.AccountingSummaryResponse;
import com.ces.erp.accounting.dto.InvoiceFieldsRequest;
import com.ces.erp.accounting.dto.InvoiceRequest;
import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.service.InvoiceService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.enums.InvoiceType;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Mühasibatlıq — E-Qaimə modulu")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Bütün qaimələri gətir")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAll(
            @RequestParam(required = false) InvoiceType type) {
        List<InvoiceResponse> result = type != null
                ? invoiceService.getByType(type)
                : invoiceService.getAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Qaimələri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String types) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAllPaged(page, size, q, type, status, types)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Maliyyə xülasəsi — ümumi gəlir, xərc, xalis mənfəət")
    public ResponseEntity<ApiResponse<AccountingSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getSummary()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Qaiməni ID ilə gətir")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @GetMapping("/by-project/{projectId}")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Layihəyə aid bütün qaimələri gətir")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByProjectId(projectId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Yeni qaimə yarat")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody InvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə əlavə edildi", invoiceService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Qaiməni yenilə")
    public ResponseEntity<ApiResponse<InvoiceResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə yeniləndi", invoiceService.update(id, req)));
    }

    @PatchMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Qaimənin inzibati sahələrini doldur (ETaxes ID, nömrə, tarix, qeyd)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> patchFields(
            @PathVariable Long id,
            @RequestBody InvoiceFieldsRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Sahələr yeniləndi", invoiceService.patchFields(id, req)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Qaiməni təsdiqlə — layihənin maliyyəsinə gəlir olaraq əlavə edilir")
    public ResponseEntity<ApiResponse<InvoiceResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə təsdiqləndi", invoiceService.approve(id)));
    }

    @PatchMapping("/{id}/return")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Qaiməni layihəyə geri qaytar")
    public ResponseEntity<ApiResponse<InvoiceResponse>> returnToProject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə geri qaytarıldı", invoiceService.returnToProject(id)));
    }

    @PatchMapping("/{id}/draft")
    @PreAuthorize("hasAuthority('ACCOUNTING:PUT')")
    @Operation(summary = "Geri qaytarılmış qaiməni DRAFT-a çevir (tam redaktə üçün)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> returnToDraft(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə DRAFT-a çevrildi", invoiceService.returnToDraft(id)));
    }

    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Geri qaytarılmış qaiməni düzəliş edib yenidən göndər")
    public ResponseEntity<ApiResponse<InvoiceResponse>> resubmit(
            @PathVariable Long id,
            @RequestBody InvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Qaimə yenidən göndərildi", invoiceService.resubmit(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING:DELETE')")
    @Operation(summary = "Qaiməni sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Qaimə silindi"));
    }

    @PostMapping(value = "/{id}/akt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Təhvil-Təslim Aktı yüklə")
    public ResponseEntity<ApiResponse<InvoiceResponse>> uploadAkt(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Akt yükləndi", invoiceService.uploadAkt(id, file)));
    }

    @GetMapping("/{id}/akt")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Təhvil-Təslim Aktını endir / preview et")
    public ResponseEntity<Resource> downloadAkt(@PathVariable Long id) throws Exception {
        Path filePath = invoiceService.resolveAktPath(id);
        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";
        String fileName = filePath.getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    // ─── Toplu qaimə: hər texnika sətrinin öz təhvil-təslim aktı ─────────────

    @PostMapping(value = "/{id}/lines/{lineId}/akt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ACCOUNTING:POST')")
    @Operation(summary = "Bir texnika sətrinin təhvil-təslim aktını yüklə")
    public ResponseEntity<ApiResponse<InvoiceResponse>> uploadLineAkt(
            @PathVariable Long id, @PathVariable Long lineId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Akt yükləndi", invoiceService.uploadLineAkt(id, lineId, file)));
    }

    @GetMapping("/{id}/lines/{lineId}/akt")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Bir texnika sətrinin aktını endir / preview et")
    public ResponseEntity<Resource> downloadLineAkt(@PathVariable Long id, @PathVariable Long lineId) throws Exception {
        Path filePath = invoiceService.resolveLineAktPath(id, lineId);
        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";
        String fileName = filePath.getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
