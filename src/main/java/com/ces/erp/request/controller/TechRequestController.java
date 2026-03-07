package com.ces.erp.request.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.UserPrincipal;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REQUESTS:GET')")
    @Operation(summary = "Sorğunu ID ilə gətir")
    public ResponseEntity<ApiResponse<TechRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getById(id)));
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
