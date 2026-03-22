package com.ces.erp.garage.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.garage.dto.*;
import com.ces.erp.garage.service.EquipmentService;
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
@RequestMapping("/api/garage/equipment")
@RequiredArgsConstructor
@Tag(name = "Garage", description = "Qaraj — texnika idarəetməsi")
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ─── Equipment CRUD ────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Bütün texnikaları gətir")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Texnikanı ID ilə gətir (baxışlar və sənədlərlə)")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GARAGE:POST')")
    @Operation(summary = "Yeni texnika əlavə et")
    public ResponseEntity<ApiResponse<EquipmentResponse>> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Texnika əlavə edildi", equipmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GARAGE:PUT')")
    @Operation(summary = "Texnikanı yenilə")
    public ResponseEntity<ApiResponse<EquipmentResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Texnika yeniləndi", equipmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GARAGE:DELETE')")
    @Operation(summary = "Texnikanı sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Texnika silindi"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('GARAGE:PUT')")
    @Operation(summary = "Texnikanın statusunu dəyiş (səbəblə)")
    public ResponseEntity<ApiResponse<EquipmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Status yeniləndi",
                equipmentService.updateStatus(id, request.getStatus(), request.getReason(), principal.getId())));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Texnikanın status tarixçəsi")
    public ResponseEntity<ApiResponse<List<StatusLogResponse>>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getStatusHistory(id)));
    }

    // ─── Filtr endpointləri ───────────────────────────────────────────────────

    @GetMapping("/by-contractor/{contractorId}")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Podratçıya aid texnikaları gətir")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getByContractor(@PathVariable Long contractorId) {
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getByContractor(contractorId)));
    }

    @GetMapping("/by-investor")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "İnvestora aid texnikaları gətir (VÖEN və ya ad ilə)")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getByInvestor(
            @RequestParam(required = false) String voen,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getByInvestor(voen, name)));
    }

    // ─── Layihə tarixçəsi (popup) ─────────────────────────────────────────────

    @GetMapping("/{id}/project-history")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Texnikanın layihə tarixçəsi")
    public ResponseEntity<ApiResponse<List<ProjectHistoryResponse>>> getProjectHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getProjectHistory(id)));
    }

    // ─── Texniki baxışlar ─────────────────────────────────────────────────────

    @PostMapping("/{id}/inspections")
    @PreAuthorize("hasAuthority('GARAGE:POST')")
    @Operation(summary = "Texniki baxış əlavə et")
    public ResponseEntity<ApiResponse<InspectionResponse>> addInspection(
            @PathVariable Long id,
            @RequestBody @Valid InspectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Baxış əlavə edildi",
                equipmentService.addInspection(id, request, null)));
    }

    @PostMapping(value = "/{id}/inspections/{inspectionId}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('GARAGE:POST')")
    @Operation(summary = "Baxış aktını yüklə")
    public ResponseEntity<ApiResponse<InspectionResponse>> uploadInspectionDocument(
            @PathVariable Long id,
            @PathVariable Long inspectionId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yükləndi",
                equipmentService.uploadInspectionDocument(id, inspectionId, file)));
    }

    @DeleteMapping("/{id}/inspections/{inspectionId}")
    @PreAuthorize("hasAuthority('GARAGE:DELETE')")
    @Operation(summary = "Texniki baxışı sil")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(@PathVariable Long id,
                                                               @PathVariable Long inspectionId) {
        equipmentService.deleteInspection(id, inspectionId);
        return ResponseEntity.ok(ApiResponse.ok("Baxış silindi"));
    }

    @GetMapping("/{id}/inspections/{inspectionId}/download")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Baxış aktını endir")
    public ResponseEntity<Resource> downloadInspectionDoc(@PathVariable Long id,
                                                           @PathVariable Long inspectionId) {
        try {
            Path path = equipmentService.resolveInspectionDocPath(id, inspectionId);
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw new com.ces.erp.common.exception.BusinessException("Fayl endirilə bilmədi");
        }
    }

    // ─── Texniki sənədlər ─────────────────────────────────────────────────────

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('GARAGE:POST')")
    @Operation(summary = "Texniki sənəd yüklə")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sənəd yükləndi",
                equipmentService.uploadDocument(id, file, documentName, principal.getId())));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('GARAGE:DELETE')")
    @Operation(summary = "Sənədi sil")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id,
                                                             @PathVariable Long documentId) {
        equipmentService.deleteDocument(id, documentId);
        return ResponseEntity.ok(ApiResponse.ok("Sənəd silindi"));
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Sənədi endir")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id,
                                                      @PathVariable Long documentId) {
        try {
            Path path = equipmentService.resolveDocumentPath(id, documentId);
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw new com.ces.erp.common.exception.BusinessException("Fayl endirilə bilmədi");
        }
    }

    // ─── Texnikanın şəkilləri ─────────────────────────────────────────────────

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('GARAGE:POST')")
    @Operation(summary = "Texnika şəkli yüklə")
    public ResponseEntity<ApiResponse<com.ces.erp.garage.dto.ImageResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Şəkil yükləndi",
                equipmentService.uploadImage(id, file, principal.getId())));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('GARAGE:DELETE')")
    @Operation(summary = "Texnika şəklini sil")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id,
                                                          @PathVariable Long imageId) {
        equipmentService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Şəkil silindi"));
    }

    @GetMapping("/{id}/images/{imageId}/view")
    @PreAuthorize("hasAuthority('GARAGE:GET')")
    @Operation(summary = "Texnika şəklini göstər (inline)")
    public ResponseEntity<Resource> viewImage(@PathVariable Long id,
                                               @PathVariable Long imageId) {
        try {
            Path path = equipmentService.resolveImagePath(id, imageId);
            Resource resource = new UrlResource(path.toUri());
            String ct = resource.getURL().openConnection().getContentType();
            MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            throw new com.ces.erp.common.exception.BusinessException("Şəkil göstərilə bilmədi");
        }
    }
}
