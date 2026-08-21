package com.ces.erp.common.notification.service;

import com.ces.erp.common.notification.dto.ErrorReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramBotService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    @Value("${app.telegram.enabled:true}")
    private boolean enabled;

    public TelegramBotService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return enabled && botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
    }

    public boolean sendErrorReport(ErrorReportRequest report) {
        if (!isConfigured()) {
            logger.warn("Telegram Bot konfiqurasiya edilməyib (botToken və ya chatId boşdur). Bildiriş göndərilmədi.");
            return false;
        }

        try {
            String message = formatErrorMessage(report);
            return sendMessageToTelegram(message);
        } catch (Exception e) {
            logger.error("Telegram xəta bildirişi göndərilərkən xəta baş verdi: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean sendMessageToTelegram(String htmlMessage) {
        if (!isConfigured()) {
            logger.warn("Telegram Bot aktiv deyil və ya parametrlər boşdur.");
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken.trim() + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId.trim());
            body.put("text", htmlMessage);
            body.put("parse_mode", "HTML");
            body.put("disable_web_page_preview", true);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Telegram bildirişi uğurla göndərildi. Status: {}", response.statusCode());
                return true;
            } else {
                logger.error("Telegram API xətası: Status {}, Cavab: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            logger.error("Telegram mesajı göndərilərkən şəbəkə xətası: {}", e.getMessage(), e);
            return false;
        }
    }

    private String formatErrorMessage(ErrorReportRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 <b>CES ERP — XƏTA BİLDİRİŞİ</b>\n\n");

        String time = (r.getTimestamp() != null && !r.getTimestamp().isBlank())
                ? r.getTimestamp()
                : LocalDateTime.now().format(DATE_FORMATTER);
        sb.append("⏰ <b>Tarix:</b> ").append(escapeHtml(time)).append("\n");

        if (r.getUserEmail() != null && !r.getUserEmail().isBlank()) {
            String userDisplay = r.getUserName() != null && !r.getUserName().isBlank()
                    ? r.getUserName() + " (" + r.getUserEmail() + ")"
                    : r.getUserEmail();
            if (r.getUserRole() != null && !r.getUserRole().isBlank()) {
                userDisplay += " [" + r.getUserRole() + "]";
            }
            sb.append("👤 <b>İstifadəçi:</b> ").append(escapeHtml(userDisplay)).append("\n");
        } else {
            sb.append("👤 <b>İstifadəçi:</b> Anonim / Daxil olmayıb\n");
        }

        if (r.getPageUrl() != null && !r.getPageUrl().isBlank()) {
            sb.append("📍 <b>Səhifə:</b> ").append(escapeHtml(r.getPageUrl())).append("\n");
        }

        if (r.getRequestUrl() != null && !r.getRequestUrl().isBlank()) {
            String method = r.getRequestMethod() != null ? r.getRequestMethod().toUpperCase() + " " : "";
            sb.append("🌐 <b>API Sorğusu:</b> <code>").append(escapeHtml(method + r.getRequestUrl())).append("</code>\n");
        }

        if (r.getHttpStatus() != null && r.getHttpStatus() > 0) {
            sb.append("⚠️ <b>HTTP Status:</b> <code>").append(r.getHttpStatus()).append("</code>\n");
        }

        sb.append("\n❌ <b>Xəta Mesajı:</b>\n");
        sb.append("<code>").append(escapeHtml(r.getErrorMessage() != null ? r.getErrorMessage() : "Xəta mətni təqdim olunmayıb")).append("</code>\n");

        if (r.getErrorDetails() != null && !r.getErrorDetails().isBlank()) {
            String details = r.getErrorDetails();
            if (details.length() > 1000) {
                details = details.substring(0, 1000) + "... (qısaldılıb)";
            }
            sb.append("\n🔍 <b>Texniki Detallar:</b>\n");
            sb.append("<pre>").append(escapeHtml(details)).append("</pre>\n");
        }

        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
