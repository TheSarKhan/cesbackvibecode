package com.ces.erp.common.seeder;

import com.ces.erp.config.entity.ConfigItem;
import com.ces.erp.config.repository.ConfigItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class DocumentConfigSeeder implements CommandLineRunner {

    private final ConfigItemRepository configRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Mövcuddursa atlayır
        if (configRepository.existsByCategoryAndKeyAndDeletedFalse("DOCUMENT_VAT_RATE", "DEFAULT")) return;

        log.info("Sənəd konfiqurasiya elementləri seed edilir...");

        List<ConfigItem> items = new ArrayList<>();

        // ─── ƏDV dərəcəsi ─────────────────────────────────────────────────────
        items.add(itemWithValue("DOCUMENT_VAT_RATE", "DEFAULT", "18", "ƏDV dərəcəsi (%)"));

        // ─── Şirkət məlumatları ────────────────────────────────────────────────
        addIfAbsent(items, "COMPANY_INFO", "COMPANY_NAME", "CES MMC");
        addIfAbsent(items, "COMPANY_INFO", "VOEN", "1703130101");
        addIfAbsent(items, "COMPANY_INFO", "ADDRESS", "Bakı şəh.");
        addIfAbsent(items, "COMPANY_INFO", "DIRECTOR_NAME", "");
        addIfAbsent(items, "COMPANY_INFO", "PHONE", "");
        addIfAbsent(items, "COMPANY_INFO", "EMAIL", "");

        // ─── Bank məlumatları ──────────────────────────────────────────────────
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "BANK_NAME", "Kapital Bank ASC");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "IBAN", "");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "SWIFT", "");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "CORRESPONDENT_ACCOUNT", "");

        configRepository.saveAll(items);
        log.info("{} sənəd konfiqurasiya elementi əlavə edildi.", items.size());
    }

    private void addIfAbsent(List<ConfigItem> list, String category, String key, String value) {
        if (!configRepository.existsByCategoryAndKeyAndDeletedFalse(category, key)) {
            list.add(itemWithValue(category, key, value, null));
        }
    }

    private ConfigItem itemWithValue(String category, String key, String value, String description) {
        return ConfigItem.builder()
                .category(category)
                .key(key)
                .value(value)
                .description(description)
                .sortOrder(0)
                .active(true)
                .build();
    }
}
