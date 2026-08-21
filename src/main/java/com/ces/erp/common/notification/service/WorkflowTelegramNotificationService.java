package com.ces.erp.common.notification.service;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.project.entity.Project;
import com.ces.erp.request.entity.TechRequest;
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
import java.util.concurrent.CompletableFuture;

@Service
public class WorkflowTelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTelegramNotificationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.telegram.workflow-bot-token:8337881314:AAHc6W9bPc5n3uJfo9r6p-IwRodArCgZk9U}")
    private String botToken;

    @Value("${app.telegram.workflow-chat-id:1133384115}")
    private String chatId;

    @Value("${app.telegram.enabled:true}")
    private boolean enabled;

    public WorkflowTelegramNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public boolean isConfigured() {
        return enabled && botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
    }

    public void notifyNewRequest(TechRequest req) {
        String msg = String.format(
                "📥 <b>YENİ SORĞU YARADILDI</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔖 <b>Kod:</b> <code>%s</code>\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "🚜 <b>Tələb olunan:</b> %s\n" +
                "📍 <b>Bölgə:</b> %s\n" +
                "📅 <b>Müddət:</b> %s gün\n" +
                "👤 <b>Əlaqə:</b> %s (%s)\n" +
                "🕒 <b>Zaman:</b> %s\n" +
                "👉 <i>Layihə Menecerinin baxışı gözlənilir.</i>",
                escape(req.getRequestCode()),
                escape(req.getCompanyName()),
                escape(req.getSelectedEquipment() != null ? req.getSelectedEquipment().getName() : (req.getProjectName() != null ? req.getProjectName() : "Qeyd edilməyib")),
                escape(req.getRegion() != null ? req.getRegion() : "-"),
                req.getDayCount() != null ? req.getDayCount() : "-",
                escape(req.getContactPerson() != null ? req.getContactPerson() : "-"),
                escape(req.getContactPhone() != null ? req.getContactPhone() : "-"),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    public void notifyCoordinatorPlanRequested(TechRequest req) {
        String msg = String.format(
                "📋 <b>KOORDİNATOR PLANI GÖZLƏNİLİR</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔖 <b>Sorğu:</b> <code>%s</code> (%s)\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "📍 <b>Bölgə:</b> %s\n" +
                "🕒 <b>Zaman:</b> %s\n" +
                "👉 <i>Shortlist tərtib edildi. Koordinator texniki plan və qiyməti təqdim etməlidir.</i>",
                escape(req.getRequestCode()),
                escape(req.getProjectName() != null ? req.getProjectName() : "-"),
                escape(req.getCompanyName()),
                escape(req.getRegion() != null ? req.getRegion() : "-"),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    public void notifyOfferSubmitted(TechRequest req, CoordinatorPlan plan) {
        String eqName = plan.getSelectedEquipment() != null ? plan.getSelectedEquipment().getName() : "-";
        String price = plan.getCustomerEquipmentPrice() != null ? plan.getCustomerEquipmentPrice().toString() + " AZN" : "-";
        String msg = String.format(
                "💼 <b>KOORDİNATOR TƏKLİFİ GÖNDƏRDİ</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔖 <b>Sorğu:</b> <code>%s</code>\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "🚜 <b>Seçilmiş Texnika:</b> %s\n" +
                "💰 <b>Təklif Qiyməti:</b> %s\n" +
                "🕒 <b>Zaman:</b> %s\n" +
                "👉 <i>Layihə Meneceri müştəri ilə razılaşmanı tamamlamalıdır.</i>",
                escape(req.getRequestCode()),
                escape(req.getCompanyName()),
                escape(eqName),
                escape(price),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    public void notifyAccountingDocsCheck(TechRequest req) {
        String agreed = req.getAgreedTotalPrice() != null ? req.getAgreedTotalPrice().toString() + " AZN" : "-";
        String msg = String.format(
                "📑 <b>MÜHASİBATLIQ SƏNƏD YOXLANIŞI</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔖 <b>Sorğu:</b> <code>%s</code>\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "💵 <b>Razılaşdırılmış Məbləğ:</b> %s\n" +
                "🕒 <b>Zaman:</b> %s\n" +
                "👉 <i>Müqavilə və Qiymət Protokolu yoxlanışı gözlənilir.</i>",
                escape(req.getRequestCode()),
                escape(req.getCompanyName()),
                escape(agreed),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    public void notifyProjectActivated(Project project) {
        String reqCode = project.getRequest() != null ? project.getRequest().getRequestCode() : "-";
        String client = project.getRequest() != null ? project.getRequest().getCompanyName() : "-";
        String msg = String.format(
                "🚀 <b>YENİ LAYİHƏ AKTİVLƏŞDİ!</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🏗️ <b>Layihə Kodu:</b> <code>%s</code>\n" +
                "🔖 <b>Əsas Sorğu:</b> <code>%s</code>\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "🕒 <b>Aktivləşmə:</b> %s\n" +
                "👉 <i>Koordinator tərəfindən operator təyini və yola salınma icra olunur.</i>",
                escape(project.getProjectCode()),
                escape(reqCode),
                escape(client),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    public void notifyReturn(TechRequest req, String reason, String fromStage) {
        String msg = String.format(
                "⚠️ <b>SORĞU GERİ QAYTARILDI</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔖 <b>Sorğu:</b> <code>%s</code>\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "🔄 <b>Mərhələ:</b> %s\n" +
                "📝 <b>Səbəb:</b> <i>%s</i>\n" +
                "🕒 <b>Zaman:</b> %s",
                escape(req.getRequestCode()),
                escape(req.getCompanyName()),
                escape(fromStage),
                escape(reason != null ? reason : "Səbəb göstərilməyib"),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    public void notifyProjectCompleted(Project project, Double finalCounter, String notes) {
        String reqCode = project.getRequest() != null ? project.getRequest().getRequestCode() : "-";
        String client = project.getRequest() != null ? project.getRequest().getCompanyName() : "-";
        String msg = String.format(
                "🏁 <b>LAYİHƏ TAMAMLANDI VƏ QARAJA QAYTARILDI</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🏗️ <b>Layihə:</b> <code>%s</code> (%s)\n" +
                "🏢 <b>Müştəri:</b> %s\n" +
                "⏱️ <b>Yekun Sayğac:</b> %s\n" +
                "📝 <b>Qeyd:</b> %s\n" +
                "🕒 <b>Tarix:</b> %s\n" +
                "👉 <i>Texnika yenidən qarajda sərbəstdir (AVAILABLE).</i>",
                escape(project.getProjectCode()),
                escape(reqCode),
                escape(client),
                finalCounter != null ? finalCounter + " Saat/KM" : "-",
                escape(notes != null ? notes : "-"),
                LocalDateTime.now().format(DATE_FMT)
        );
        sendAsync(msg);
    }

    private void sendAsync(String htmlMessage) {
        if (!isConfigured()) return;
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken.trim() + "/sendMessage";
                Map<String, Object> body = new HashMap<>();
                body.put("chat_id", chatId.trim());
                body.put("text", htmlMessage);
                body.put("parse_mode", "HTML");
                body.put("disable_web_page_preview", true);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(6))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();

                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    log.warn("Telegram workflow bot cavab verdi: {} {}", resp.statusCode(), resp.body());
                }
            } catch (Exception e) {
                log.warn("Telegram workflow bildirişi göndərilmədi: {}", e.getMessage());
            }
        });
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
