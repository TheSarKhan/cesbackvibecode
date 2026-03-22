package com.ces.erp.request.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.dto.RequestStatusChangeRequest;
import com.ces.erp.request.dto.StatusLogResponse;
import com.ces.erp.request.dto.TechRequestRequest;
import com.ces.erp.request.dto.TechRequestResponse;
import com.ces.erp.request.service.TechRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Requests", description = "Sorğu idarəetməsi")
public class TechRequestController {

    private final TechRequestService requestService;

    @GetMapping
    @PreAuthorize("hasAuthority('REQUESTS:GET')")
    @Operation(summary = "Bütün sorğuları gətir")
    public ResponseEntity<ApiResponse<List<TechRequestResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(requestService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('REQUESTS:GET')")
    @Operation(summary = "Sorğuları səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<TechRequestResponse>>> getAllPaged(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String projectType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.getAllPaged(search, status, region, projectType, page, size, sortBy, sortDir)));
    }

    @GetMapping("/status-transitions")
    @PreAuthorize("hasAuthority('REQUESTS:GET')")
    @Operation(summary = "İcazə verilən status keçidlərini gətir")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getStatusTransitions() {
        return ResponseEntity.ok(ApiResponse.success(requestService.getAllowedTransitions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REQUESTS:GET')")
    @Operation(summary = "Sorğunu ID ilə gətir")
    public ResponseEntity<ApiResponse<TechRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('REQUESTS:GET')")
    @Operation(summary = "Sorğunun status tarixçəsini gətir")
    public ResponseEntity<ApiResponse<List<StatusLogResponse>>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getStatusHistory(id)));
    }

    @PostMapping("/{id}/change-status")
    @PreAuthorize("hasAuthority('REQUESTS:PUT')")
    @Operation(summary = "Sorğunun statusunu dəyiş")
    public ResponseEntity<ApiResponse<TechRequestResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody RequestStatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status dəyişdirildi",
                requestService.changeStatus(id, request.getStatus(), request.getReason())));
    }

    @PostMapping("/bulk-update-notes")
    @PreAuthorize("hasAuthority('REQUESTS:PUT')")
    @Operation(summary = "Toplu qeyd yenilə")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateNotes(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) body.get("ids")).stream().map(Number::longValue).toList();
        String notes = (String) body.get("notes");
        requestService.bulkUpdateNotes(ids, notes);
        return ResponseEntity.ok(ApiResponse.ok("Toplu yeniləmə tamamlandı"));
    }

    @PostMapping("/bulk-update-region")
    @PreAuthorize("hasAuthority('REQUESTS:PUT')")
    @Operation(summary = "Toplu bölgə yenilə")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateRegion(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) body.get("ids")).stream().map(Number::longValue).toList();
        String region = (String) body.get("region");
        requestService.bulkUpdateRegion(ids, region);
        return ResponseEntity.ok(ApiResponse.ok("Toplu yeniləmə tamamlandı"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REQUESTS:POST')")
    @Operation(summary = "Yeni sorğu yarat")
    public ResponseEntity<ApiResponse<TechRequestResponse>> create(
            @Valid @RequestBody TechRequestRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu yaradıldı",
                requestService.create(request, principal.getId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REQUESTS:PUT')")
    @Operation(summary = "Sorğunu yenilə")
    public ResponseEntity<ApiResponse<TechRequestResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TechRequestRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu yeniləndi",
                requestService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('REQUESTS:PUT')")
    @Operation(summary = "Sorğunu Alım-Satım komandasına göndər (DRAFT → PENDING)")
    public ResponseEntity<ApiResponse<TechRequestResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu göndərildi",
                requestService.submit(id)));
    }

    @PutMapping("/{id}/equipment")
    @PreAuthorize("hasAuthority('REQUESTS:PUT')")
    @Operation(summary = "Sorğuya texnika seç")
    public ResponseEntity<ApiResponse<TechRequestResponse>> selectEquipment(
            @PathVariable Long id,
            @RequestParam Long equipmentId) {
        return ResponseEntity.ok(ApiResponse.success("Texnika seçildi",
                requestService.selectEquipment(id, equipmentId)));
    }

    @PostMapping("/{id}/send-to-coordinator")
    @PreAuthorize("hasAuthority('REQUESTS:SEND_COORDINATOR')")
    @Operation(summary = "Sorğunu kordinatora göndər (PENDING → SENT_TO_COORDINATOR)")
    public ResponseEntity<ApiResponse<TechRequestResponse>> sendToCoordinator(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Kordinatora göndərildi",
                requestService.sendToCoordinator(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REQUESTS:DELETE')")
    @Operation(summary = "Sorğunu sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        requestService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Sorğu silindi"));
    }
}
