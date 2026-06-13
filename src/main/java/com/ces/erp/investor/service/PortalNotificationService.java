package com.ces.erp.investor.service;

import com.ces.erp.accounting.entity.Payable;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.investor.dto.NotificationResponse;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.entity.InvestorNotification;
import com.ces.erp.investor.entity.InvestorPushToken;
import com.ces.erp.investor.repository.InvestorNotificationRepository;
import com.ces.erp.investor.repository.InvestorPushTokenRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * İnvestor bildirişləri — DB-də saxla + Expo push göndər.
 * Trigger metodları (onEquipmentRented/onPaymentReceived) REQUIRES_NEW + try/catch ilə
 * tam izolədir: bildiriş xətası ERP əməliyyatını (status/ödəniş) GERİ ALMAMALIDIR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalNotificationService {

    private final InvestorNotificationRepository notificationRepository;
    private final InvestorPushTokenRepository pushTokenRepository;
    private final InvestorRepository investorRepository;
    private final ExpoPushService expoPushService;

    // ─── Token idarəetməsi ────────────────────────────────────────────────────

    @Transactional
    public void registerToken(Long investorId, String token, String platform) {
        Investor investor = investorRepository.findByIdAndDeletedFalse(investorId)
                .orElseThrow(() -> new IllegalStateException("İnvestor tapılmadı"));
        InvestorPushToken existing = pushTokenRepository.findByToken(token).orElse(null);
        if (existing != null) {
            existing.setInvestor(investor);
            existing.setPlatform(platform);
            pushTokenRepository.save(existing);
        } else {
            pushTokenRepository.save(InvestorPushToken.builder()
                    .investor(investor)
                    .token(token)
                    .platform(platform)
                    .build());
        }
    }

    @Transactional
    public void removeToken(String token) {
        try {
            pushTokenRepository.deleteByToken(token);
        } catch (Exception e) {
            log.warn("[Notif] token silinmədi: {}", e.getMessage());
        }
    }

    // ─── Bildiriş siyahısı / oxunma ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long investorId) {
        return notificationRepository.findTop50ByInvestorIdOrderByCreatedAtDesc(investorId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long investorId) {
        return notificationRepository.countByInvestorIdAndReadFalse(investorId);
    }

    @Transactional
    public void markRead(Long investorId, Long id) {
        notificationRepository.findByIdAndInvestorId(id, investorId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(Long investorId) {
        notificationRepository.markAllRead(investorId);
    }

    // ─── Triggerlər (best-effort, izolə) ────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEquipmentRented(Equipment e) {
        try {
            if (e == null || e.getOwnershipType() != OwnershipType.INVESTOR
                    || e.getOwnerInvestorVoen() == null) return;
            investorRepository.findByVoenAndDeletedFalse(e.getOwnerInvestorVoen()).ifPresent(inv ->
                    create(inv, "Texnika icarəyə verildi",
                            (e.getName() != null ? e.getName() : "Texnika") + " indi icarədədir.",
                            "EQUIPMENT_RENTED", e.getId()));
        } catch (Exception ex) {
            log.warn("[Notif] onEquipmentRented xətası: {}", ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentReceived(Payable p, BigDecimal amount) {
        try {
            if (p == null || p.getContractor() != null || p.getInvestorVoen() == null) return;
            investorRepository.findByVoenAndDeletedFalse(p.getInvestorVoen()).ifPresent(inv ->
                    create(inv, "Ödəniş daxil oldu",
                            fmtAzn(amount) + " məbləğində ödəniş qeydə alındı.",
                            "PAYMENT_RECEIVED", p.getId()));
        } catch (Exception ex) {
            log.warn("[Notif] onPaymentReceived xətası: {}", ex.getMessage());
        }
    }

    // ─── Daxili ──────────────────────────────────────────────────────────────────

    private void create(Investor investor, String title, String body, String type, Long relatedId) {
        notificationRepository.save(InvestorNotification.builder()
                .investor(investor)
                .title(title)
                .body(body)
                .type(type)
                .relatedId(relatedId)
                .read(false)
                .build());

        List<String> tokens = pushTokenRepository.findAllByInvestorId(investor.getId()).stream()
                .map(InvestorPushToken::getToken)
                .toList();
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        if (relatedId != null) data.put("id", relatedId);
        expoPushService.send(tokens, title, body, data);
    }

    private static String fmtAzn(BigDecimal v) {
        if (v == null) return "0 ₼";
        return v.stripTrailingZeros().toPlainString() + " ₼";
    }
}
