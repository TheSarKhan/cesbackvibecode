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
        log.info("Sənəd konfiqurasiya elementləri yoxlanır...");

        List<ConfigItem> items = new ArrayList<>();

        // ─── ƏDV dərəcəsi ─────────────────────────────────────────────────────
        addIfAbsent(items, "DOCUMENT_VAT_RATE", "DEFAULT", "18");

        // ─── Şirkət məlumatları ────────────────────────────────────────────────
        addIfAbsent(items, "COMPANY_INFO", "COMPANY_NAME",  "Construction Equipment Services MMC");
        addIfAbsent(items, "COMPANY_INFO", "VOEN",          "1703130101");
        addIfAbsent(items, "COMPANY_INFO", "ADDRESS",       "Bakı şəh., Azərbaycan");
        addIfAbsent(items, "COMPANY_INFO", "DIRECTOR_NAME", "Elvin Seyidov");
        addIfAbsent(items, "COMPANY_INFO", "PHONE", "");
        addIfAbsent(items, "COMPANY_INFO", "EMAIL", "");

        // ─── Bank məlumatları ──────────────────────────────────────────────────
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "BANK_NAME",             "Azər-Türk Bank");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "BANK_CODE",             "507699");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "SWIFT",                 "AZRTAZ22XXX");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "IBAN",                  "AZ69AZRT40060019440587075001");
        addIfAbsent(items, "COMPANY_BANK_DETAILS", "CORRESPONDENT_ACCOUNT", "AZ02NABZ01350100000000022944");

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
