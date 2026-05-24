package com.ces.erp.projectmanager.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.projectmanager.dto.CustomerAgreementRequest;
import com.ces.erp.projectmanager.dto.PmRequestResponse;
import com.ces.erp.projectmanager.dto.ShortlistSaveRequest;
import com.ces.erp.projectmanager.service.ProjectManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project-manager")
@RequiredArgsConstructor
@Tag(name = "Project Manager", description = "Layihə Meneceri əməliyyatları (shortlist, qiymət razılaşması, təsdiq)")
public class ProjectManagerController {

    private final ProjectManagerService service;

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "PM-in görəcəyi sorğular (filter olunmamış)")
    public ResponseEntity<ApiResponse<List<PmRequestResponse>>> getRequests() {
        return ResponseEntity.ok(ApiResponse.success(service.getRequests()));
    }

    @GetMapping("/requests/paged")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "Sorğular - səhifələnmiş")
    public ResponseEntity<ApiResponse<PagedResponse<PmRequestResponse>>> getRequestsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getRequestsPaged(page, size, search, status, sortBy, sortDir)));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:GET')")
    @Operation(summary = "Sorğu detalı (shortlist + koordinator təklifi ilə)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> getRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(service.getRequest(requestId)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Sorğunu qəbul et (PENDING → PM_REVIEW)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> accept(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu qəbul edildi", service.accept(requestId)));
    }

    @PostMapping("/requests/{requestId}/shortlist")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:POST')")
    @Operation(summary = "Shortlist sətirlərini saxla (upsert)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveShortlist(
            @PathVariable Long requestId,
            @RequestBody ShortlistSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shortlist yadda saxlandı",
                service.saveShortlist(requestId, request)));
    }

    @DeleteMapping("/requests/{requestId}/shortlist/items/{itemId}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:DELETE')")
    @Operation(summary = "Shortlist sətirini sil")
    public ResponseEntity<ApiResponse<Void>> deleteShortlistItem(
            @PathVariable Long requestId, @PathVariable Long itemId) {
        service.deleteShortlistItem(requestId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Sətir silindi"));
    }

    @PostMapping("/requests/{requestId}/send-to-coordinator")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Shortlisti koordinatora göndər (status → COORDINATOR_NEGOTIATING)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> sendToCoordinator(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Koordinatora göndərildi",
                service.sendToCoordinator(requestId)));
    }

    @PutMapping("/requests/{requestId}/customer-agreement")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Sifarişçi ilə razılaşdırılmış qiyməti və qeydi saxla")
    public ResponseEntity<ApiResponse<PmRequestResponse>> saveCustomerAgreement(
            @PathVariable Long requestId,
            @RequestBody CustomerAgreementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Razılaşma qeyd edildi",
                service.saveCustomerAgreement(requestId, request)));
    }

    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:APPROVE_PM')")
    @Operation(summary = "Sorğunu təsdiqlə (Layihə yaradılır, mühasibatlığa göndərilir)")
    public ResponseEntity<ApiResponse<PmRequestResponse>> approve(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Sorğu təsdiqləndi", service.approve(requestId)));
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER:PUT')")
    @Operation(summary = "Sorğunu rədd et")
    public ResponseEntity<ApiResponse<PmRequestResponse>> reject(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Sorğu rədd edildi", service.reject(requestId, reason)));
    }
}
