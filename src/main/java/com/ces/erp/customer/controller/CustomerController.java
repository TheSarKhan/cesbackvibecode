package com.ces.erp.customer.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.customer.dto.CustomerDocumentResponse;
import com.ces.erp.customer.dto.CustomerRequest;
import com.ces.erp.customer.dto.CustomerResponse;
import com.ces.erp.customer.service.CustomerService;
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

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Müştəri idarəetməsi")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:GET')")
    @Operation(summary = "Bütün müştəriləri gətir")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:GET')")
    @Operation(summary = "Müştərini ID ilə gətir")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:POST')")
    @Operation(summary = "Yeni müştəri əlavə et")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Müştəri əlavə edildi", customerService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:PUT')")
    @Operation(summary = "Müştərini yenilə")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Müştəri yeniləndi", customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:DELETE')")
    @Operation(summary = "Müştərini sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Müştəri silindi"));
    }

    // ─── Sənədlər ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:POST')")
    @Operation(summary = "Sənəd yüklə")
    public ResponseEntity<ApiResponse<CustomerDocumentResponse>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "documentDate", required = false) String documentDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yükləndi",
                customerService.uploadDocument(id, file, documentName, documentDate, principal.getId())));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:DELETE')")
    @Operation(summary = "Sənədi sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id,
                                                             @PathVariable Long documentId) {
        customerService.deleteDocument(id, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGEMENT:GET')")
    @Operation(summary = "Sənədi endir")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id,
                                                      @PathVariable Long documentId) {
        try {
            Path path = customerService.resolveDocumentPath(id, documentId);
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw new com.ces.erp.common.exception.BusinessException("Fayl endirilə bilmədi");
        }
    }
}
