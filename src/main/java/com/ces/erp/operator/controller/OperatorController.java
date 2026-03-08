package com.ces.erp.operator.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.enums.OperatorDocumentType;
import com.ces.erp.operator.dto.OperatorRequest;
import com.ces.erp.operator.dto.OperatorResponse;
import com.ces.erp.operator.service.OperatorService;
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

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
@Tag(name = "Operators", description = "Operator idarəetməsi")
public class OperatorController {

    private final OperatorService operatorService;

    @GetMapping
    @PreAuthorize("hasAuthority('OPERATORS:GET')")
    public ResponseEntity<ApiResponse<List<OperatorResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(operatorService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATORS:GET')")
    public ResponseEntity<ApiResponse<OperatorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(operatorService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPERATORS:POST')")
    public ResponseEntity<ApiResponse<OperatorResponse>> create(@Valid @RequestBody OperatorRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Operator əlavə edildi", operatorService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATORS:PUT')")
    public ResponseEntity<ApiResponse<OperatorResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody OperatorRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Operator yeniləndi", operatorService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATORS:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        operatorService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Operator silindi"));
    }

    // ─── Sənədlər ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('OPERATORS:POST')")
    public ResponseEntity<ApiResponse<OperatorResponse>> uploadDocument(
            @PathVariable Long id,
            @PathVariable OperatorDocumentType type,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yükləndi",
                operatorService.uploadDocument(id, type, file)));
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('OPERATORS:DELETE')")
    public ResponseEntity<ApiResponse<OperatorResponse>> deleteDocument(@PathVariable Long id,
                                                                        @PathVariable Long docId) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd silindi",
                operatorService.deleteDocument(id, docId)));
    }

    @GetMapping("/{id}/documents/{docId}/download")
    @PreAuthorize("hasAuthority('OPERATORS:GET')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id,
                                                     @PathVariable Long docId) {
        try {
            Path path = operatorService.resolveDocumentPath(id, docId);
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
