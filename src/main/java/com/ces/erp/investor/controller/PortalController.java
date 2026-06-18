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
import com.ces.erp.investor.dto.NotificationResponse;
import com.ces.erp.investor.dto.PortalDashboardResponse;
import com.ces.erp.investor.dto.PortalEquipmentEarnings;
import com.ces.erp.investor.dto.PushTokenRequest;
import com.ces.erp.investor.service.PortalNotificationService;
import com.ces.erp.investor.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.ces.erp.common.exception.FileStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final PortalNotificationService notificationService;

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

    @GetMapping("/equipment/{id}/earnings")
    @Operation(summary = "Avadanlığın qazancı (cəm, aylıq, trend, işləklik)")
    public ResponseEntity<ApiResponse<PortalEquipmentEarnings>> equipmentEarnings(
            @AuthenticationPrincipal InvestorPrincipal me, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getEquipmentEarnings(me.getInvestorId(), id)));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Qaimələrim")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> invoices(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getInvoices(me.getInvestorId())));
    }

    @GetMapping("/invoices/{id}/payments")
    @Operation(summary = "Bu qaimə üzrə edilmiş ödənişlər")
    public ResponseEntity<ApiResponse<List<com.ces.erp.accounting.dto.PayablePaymentResponse>>> invoicePayments(
            @AuthenticationPrincipal InvestorPrincipal me, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getInvoicePayments(me.getInvestorId(), id)));
    }

    @GetMapping("/payments")
    @Operation(summary = "Ödənişlər / borc")
    public ResponseEntity<ApiResponse<List<PayableResponse>>> payments(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getPayments(me.getInvestorId())));
    }

    @GetMapping("/documents")
    @Operation(summary = "Bütün sənədlərim (müqavilə, akt, texnika sənədi, qaimə)")
    public ResponseEntity<ApiResponse<List<com.ces.erp.partydoc.PartyDocumentDto>>> documents(
            @AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getDocuments(me.getInvestorId())));
    }

    @GetMapping("/documents/{sourceType}/{sourceId}/download")
    @Operation(summary = "Sənədi endir (yalnız öz sənədim)")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal InvestorPrincipal me,
            @PathVariable String sourceType, @PathVariable Long sourceId) {
        try {
            var df = portalService.resolveHubDocument(me.getInvestorId(), sourceType, sourceId);
            Path path = df.path();
            Resource resource = new UrlResource(path.toUri());
            String ct = Files.probeContentType(path);
            MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;
            String fileName = df.fileName() != null ? df.fileName() : path.getFileName().toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(mediaType)
                    .body(resource);
        } catch (IOException e) {
            throw new FileStorageException("Fayl endirilə bilmədi: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Şifrəmi dəyiş")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal InvestorPrincipal me,
            @Valid @RequestBody InvestorChangePasswordRequest request) {
        portalService.changePassword(me.getInvestorId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Şifrə dəyişdirildi"));
    }

    // ─── Push token + bildirişlər ────────────────────────────────────────────

    @PostMapping("/push-token")
    @Operation(summary = "Push token qeydiyyatı")
    public ResponseEntity<ApiResponse<Void>> registerPushToken(
            @AuthenticationPrincipal InvestorPrincipal me, @Valid @RequestBody PushTokenRequest req) {
        notificationService.registerToken(me.getInvestorId(), req.getToken(), req.getPlatform());
        return ResponseEntity.ok(ApiResponse.ok("Token qeydə alındı"));
    }

    @PostMapping("/push-token/remove")
    @Operation(summary = "Push token sil (çıxış)")
    public ResponseEntity<ApiResponse<Void>> removePushToken(@Valid @RequestBody PushTokenRequest req) {
        notificationService.removeToken(req.getToken());
        return ResponseEntity.ok(ApiResponse.ok("Token silindi"));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Bildirişlərim")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> notifications(
            @AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.list(me.getInvestorId())));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Oxunmamış bildiriş sayı")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal InvestorPrincipal me) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.unreadCount(me.getInvestorId())));
    }

    @PostMapping("/notifications/{id}/read")
    @Operation(summary = "Bildirişi oxundu işarələ")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal InvestorPrincipal me, @PathVariable Long id) {
        notificationService.markRead(me.getInvestorId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Oxundu"));
    }

    @PostMapping("/notifications/read-all")
    @Operation(summary = "Hamısını oxundu işarələ")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal InvestorPrincipal me) {
        notificationService.markAllRead(me.getInvestorId());
        return ResponseEntity.ok(ApiResponse.ok("Hamısı oxundu"));
    }
}
