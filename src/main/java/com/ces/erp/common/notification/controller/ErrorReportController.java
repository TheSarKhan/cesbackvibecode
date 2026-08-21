package com.ces.erp.common.notification.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.notification.dto.ErrorReportRequest;
import com.ces.erp.common.notification.service.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "System Notifications", description = "Sistem xəta və bildiriş servisləri")
public class ErrorReportController {

    private final TelegramBotService telegramBotService;

    @PostMapping("/report-error")
    @Operation(summary = "Xəta bildirişini Telegram vasitəsilə administratora göndər")
    public ResponseEntity<ApiResponse<MapStatus>> reportError(
            @RequestBody ErrorReportRequest request,
            HttpServletRequest servletRequest
    ) {
        // İstifadəçi daxil olubsa, auth kontekstindən məlumatları tamamla
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (request.getUserEmail() == null || request.getUserEmail().isBlank()) {
                request.setUserEmail(auth.getName());
            }
            if (request.getUserRole() == null || request.getUserRole().isBlank()) {
                request.setUserRole(auth.getAuthorities().toString());
            }
        }

        if (request.getUserAgent() == null || request.getUserAgent().isBlank()) {
            request.setUserAgent(servletRequest.getHeader("User-Agent"));
        }

        boolean sent = telegramBotService.sendErrorReport(request);

        if (sent) {
            return ResponseEntity.ok(ApiResponse.ok("Xəta bildirişi administratora uğurla çatdırıldı"));
        } else {
            // Əgər bot token hələ quraşdırılmayıbsa da istifadəçiyə aydın mesaj verək
            if (!telegramBotService.isConfigured()) {
                return ResponseEntity.ok(ApiResponse.ok("Xəta qeydə alındı (Telegram bildiriş botu hələ konfiqurasiya edilməyib)"));
            }
            return ResponseEntity.ok(ApiResponse.error("Telegram vasitəsilə göndərilmə uğursuz oldu. Zəhmət olmasa yenidən cəhd edin."));
        }
    }

    public record MapStatus(boolean sent) {}
}
