package com.ces.erp.common.seeder;

import com.ces.erp.config.entity.ConfigItem;
import com.ces.erp.config.repository.ConfigItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class ConfigSeeder implements CommandLineRunner {

    private final ConfigItemRepository configRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (configRepository.count() > 0) return;
        log.info("Konfiqurasiya elementləri seed edilir...");

        List<ConfigItem> items = List.of(
                // ─── Texnika brendləri ────────────────────────
                item("EQUIPMENT_BRAND", "CAT",        "Caterpillar",       0),
                item("EQUIPMENT_BRAND", "Komatsu",    "Komatsu",           1),
                item("EQUIPMENT_BRAND", "Volvo",      "Volvo CE",          2),
                item("EQUIPMENT_BRAND", "Hitachi",    "Hitachi",           3),
                item("EQUIPMENT_BRAND", "JCB",        "JCB",               4),
                item("EQUIPMENT_BRAND", "Liebherr",   "Liebherr",          5),
                item("EQUIPMENT_BRAND", "Doosan",     "Doosan",            6),
                item("EQUIPMENT_BRAND", "Hyundai",    "Hyundai CE",        7),
                item("EQUIPMENT_BRAND", "XCMG",       "XCMG",              8),
                item("EQUIPMENT_BRAND", "Sany",       "Sany",              9),
                item("EQUIPMENT_BRAND", "Zoomlion",   "Zoomlion",         10),

                // ─── Texnika növləri ──────────────────────────
                item("EQUIPMENT_TYPE", "Ekskavator",       "Ekskavator",       0),
                item("EQUIPMENT_TYPE", "Yükləyici",        "Yükləyici",        1),
                item("EQUIPMENT_TYPE", "Buldozer",         "Buldozer",         2),
                item("EQUIPMENT_TYPE", "Greyder",          "Greyder",          3),
                item("EQUIPMENT_TYPE", "Kran",             "Kran",             4),
                item("EQUIPMENT_TYPE", "Kompressor",       "Kompressor",       5),
                item("EQUIPMENT_TYPE", "Generator",        "Generator",        6),
                item("EQUIPMENT_TYPE", "Qazıcı",          "Qazıcı maşın",    7),
                item("EQUIPMENT_TYPE", "Betonqarışdıran", "Betonqarışdıran",  8),
                item("EQUIPMENT_TYPE", "Roller",           "Roller / Silindr", 9),
                item("EQUIPMENT_TYPE", "Forklift",         "Forklift",        10),
                item("EQUIPMENT_TYPE", "Yük maşını",      "Yük maşını",      11),

                // ─── Bölgələr ────────────────────────────────
                item("REGION", "Bakı",       "Bakı",        0),
                item("REGION", "Sumqayıt",   "Sumqayıt",    1),
                item("REGION", "Gəncə",     "Gəncə",      2),
                item("REGION", "Lənkəran",  "Lənkəran",   3),
                item("REGION", "Şəki",      "Şəki",       4),
                item("REGION", "Mingəçevir", "Mingəçevir",  5),
                item("REGION", "Naxçıvan",  "Naxçıvan",   6),
                item("REGION", "Şirvan",    "Şirvan",     7),
                item("REGION", "Quba",       "Quba",        8),
                item("REGION", "Zaqatala",   "Zaqatala",    9),

                // ─── Texniki parametr adları ─────────────────
                item("TECH_PARAM", "Çəki (ton)",       "Texnikanın çəkisi",     0),
                item("TECH_PARAM", "Güc (HP)",         "Mühərrik gücü",         1),
                item("TECH_PARAM", "Tutum (m³)",      "Çanaq tutumu",          2),
                item("TECH_PARAM", "Hündürlük (m)",   "Maksimal hündürlük",    3),
                item("TECH_PARAM", "Uzunluq (m)",      "Boom uzunluğu",         4),
                item("TECH_PARAM", "Yük qaldırma (ton)", "Yük qaldırma gücü",  5)
        );

        configRepository.saveAll(items);
        log.info("{} konfiqurasiya elementi əlavə edildi.", items.size());
    }

    private ConfigItem item(String category, String key, String value, int order) {
        return ConfigItem.builder()
                .category(category)
                .key(key)
                .value(value)
                .sortOrder(order)
                .active(true)
                .build();
    }
}
