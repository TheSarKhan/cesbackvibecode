package com.ces.erp.approval.controller;

import com.ces.erp.approval.dto.ApprovalSummaryResponse;
import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.dto.RejectRequest;
import com.ces.erp.approval.service.ApprovalService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
@Tag(name = "Approval", description = "Əməliyyatların təsdiqi")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    @PreAuthorize("hasAuthority('OPERATIONS_APPROVAL:GET')")
    @Operation(summary = "Təsdiq növbəsini al")
    public ResponseEntity<ApiResponse<List<ApprovalSummaryResponse>>> getQueue(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Əməliyyatlar yükləndi",
                approvalService.getQueue(principal.getId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATIONS_APPROVAL:GET')")
    @Operation(summary = "Əməliyyat detallarını al (diff ilə)")
    public ResponseEntity<ApiResponse<PendingOperationResponse>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Əməliyyat yükləndi",
                approvalService.getDetail(id, principal.getId())));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('OPERATIONS_APPROVAL:PUT')")
    @Operation(summary = "Əməliyyatı təsdiq et")
    public ResponseEntity<ApiResponse<ApprovalSummaryResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Əməliyyat təsdiq edildi",
                approvalService.approve(id, principal.getId())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('OPERATIONS_APPROVAL:PUT')")
    @Operation(summary = "Əməliyyatı rədd et")
    public ResponseEntity<ApiResponse<ApprovalSummaryResponse>> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RejectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Əməliyyat rədd edildi",
                approvalService.reject(id, principal.getId(), request)));
    }
}
