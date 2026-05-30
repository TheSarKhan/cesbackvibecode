package com.ces.erp.investor.controller;

import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.InvestorPrincipal;
import com.ces.erp.garage.dto.DocumentResponse;
import com.ces.erp.garage.dto.EquipmentResponse;
import com.ces.erp.garage.dto.ProjectHistoryResponse;
import com.ces.erp.investor.dto.InvestorChangePasswordRequest;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.dto.PortalDashboardResponse;
import com.ces.erp.investor.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Investor portal API — read-only (v1) + şifrə dəyişimi.
 * SecurityConfig {@code /api/portal/**} → hasRole('INVESTOR'). Hər sorğu
 * principal-dakı investorId ilə scoped olur; istemçi heç vaxt id/voen ötürmür.
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Tag(name = "Investor Portal", description = "İnvestor portal (scoped, read-only)")
public class PortalController {

    private final PortalService portalService;

    @GetMapping("/me")
    @Operation(summary = "Profilim")
    public ResponseEntity<ApiResponse<InvestorResponse>> me(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getProfile(me.getInvestorId())));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Statistika")
    public ResponseEntity<ApiResponse<PortalDashboardResponse>> dashboard(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getDashboard(me.getInvestorId())));
    }

    @GetMapping("/equipment")
    @Operation(summary = "Avadanlıqlarım")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> equipment(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getEquipment(me.getInvestorId())));
    }

    @GetMapping("/equipment/{id}")
    @Operation(summary = "Avadanlıq detalı")
    public ResponseEntity<ApiResponse<EquipmentResponse>> equipmentDetail(
            @AuthenticationPrincipal InvestorPrincipal me, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getEquipmentDetail(me.getInvestorId(), id)));
    }

    @GetMapping("/equipment/{id}/history")
    @Operation(summary = "Avadanlıq-layihə keçmişi")
    public ResponseEntity<ApiResponse<List<ProjectHistoryResponse>>> equipmentHistory(
            @AuthenticationPrincipal InvestorPrincipal me, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getEquipmentHistory(me.getInvestorId(), id)));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Qaimələrim")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> invoices(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getInvoices(me.getInvestorId())));
    }

    @GetMapping("/payments")
    @Operation(summary = "Ödənişlər / borc")
    public ResponseEntity<ApiResponse<List<PayableResponse>>> payments(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getPayments(me.getInvestorId())));
    }

    @GetMapping("/documents")
    @Operation(summary = "Avadanlıq sənədlərim")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> documents(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getDocuments(me.getInvestorId())));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Şifrəmi dəyiş")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal InvestorPrincipal me,
            @Valid @RequestBody InvestorChangePasswordRequest request) {
        portalService.changePassword(me.getInvestorId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Şifrə dəyişdirildi"));
    }
}
