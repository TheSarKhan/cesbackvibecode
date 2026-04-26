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
@Order(2)
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

                // ─── Texnika brendləri ────────────────────────────────────────
                item("EQUIPMENT_BRAND", "Caterpillar", 0),
                item("EQUIPMENT_BRAND", "Komatsu",     1),
                item("EQUIPMENT_BRAND", "Volvo CE",    2),
                item("EQUIPMENT_BRAND", "Hitachi",     3),
                item("EQUIPMENT_BRAND", "JCB",         4),
                item("EQUIPMENT_BRAND", "Liebherr",    5),
                item("EQUIPMENT_BRAND", "Doosan",      6),
                item("EQUIPMENT_BRAND", "Hyundai CE",  7),
                item("EQUIPMENT_BRAND", "XCMG",        8),
                item("EQUIPMENT_BRAND", "Sany",        9),
                item("EQUIPMENT_BRAND", "Zoomlion",   10),
                item("EQUIPMENT_BRAND", "Bobcat",     11),
                item("EQUIPMENT_BRAND", "Dynapac",    12),
                item("EQUIPMENT_BRAND", "Bomag",      13),
                item("EQUIPMENT_BRAND", "Mercedes-Benz", 14),

                // ─── Texnika növləri ──────────────────────────────────────────
                item("EQUIPMENT_TYPE", "Ekskavator",         0),
                item("EQUIPMENT_TYPE", "Yükləyici",          1),
                item("EQUIPMENT_TYPE", "Buldozer",           2),
                item("EQUIPMENT_TYPE", "Greyder",            3),
                item("EQUIPMENT_TYPE", "Kran",               4),
                item("EQUIPMENT_TYPE", "Kompressor",         5),
                item("EQUIPMENT_TYPE", "Generator",          6),
                item("EQUIPMENT_TYPE", "Qazıcı",            7),
                item("EQUIPMENT_TYPE", "Betonqarışdıran",   8),
                item("EQUIPMENT_TYPE", "Roller / Silindir",  9),
                item("EQUIPMENT_TYPE", "Forklift",          10),
                item("EQUIPMENT_TYPE", "Yük maşını",        11),
                item("EQUIPMENT_TYPE", "Mini Ekskavator",   12),
                item("EQUIPMENT_TYPE", "Teleskopik Yükləyici", 13),

                // ─── Bölgələr ────────────────────────────────────────────────
                item("REGION", "Bakı",        0),
                item("REGION", "Sumqayıt",    1),
                item("REGION", "Gəncə",      2),
                item("REGION", "Lənkəran",   3),
                item("REGION", "Şəki",       4),
                item("REGION", "Mingəçevir",  5),
                item("REGION", "Naxçıvan",   6),
                item("REGION", "Şirvan",     7),
                item("REGION", "Quba",        8),
                item("REGION", "Zaqatala",    9),
                item("REGION", "Abşeron",    10),
                item("REGION", "Xırdalan",   11),
                item("REGION", "Balaxanı",   12),

                // ─── Texniki parametr adları ──────────────────────────────────
                item("TECH_PARAM", "Texnika növü",         0),
                item("TECH_PARAM", "Çəki (ton)",           1),
                item("TECH_PARAM", "Güc (HP)",             2),
                item("TECH_PARAM", "Tutum (m³)",          3),
                item("TECH_PARAM", "Hündürlük (m)",       4),
                item("TECH_PARAM", "Yük qaldırma (ton)",  5),
                item("TECH_PARAM", "Boom uzunluğu (m)",    6),
                item("TECH_PARAM", "İş müddəti (gün)",    7),
                item("TECH_PARAM", "Torpaq növü",          8),
                item("TECH_PARAM", "İş saatları",          9),
                item("TECH_PARAM", "Kovş həcmi (m³)",    10),

                item("SAFETY_EQUIPMENT", "Əks-işıq (reflektor)", 4),

                // ─── Servis Checklist Şablonları ──────────────────────────────
                item("SERVICE_CHECKLIST", "Mühərrik yağ səviyyəsi",         0),
                item("SERVICE_CHECKLIST", "Soyutma mayesinin səviyyəsi",    1),
                item("SERVICE_CHECKLIST", "Əyləc bəndlərinin yoxlanılması", 2),
                item("SERVICE_CHECKLIST", "Şinlərin təzyiqi və vəziyyəti",  3),
                item("SERVICE_CHECKLIST", "Hidravlika sistemi (sızma)",     4)
        );

        configRepository.saveAll(items);
        log.info("{} konfiqurasiya elementi əlavə edildi.", items.size());
    }

    private ConfigItem item(String category, String key, int order) {
        return ConfigItem.builder()
                .category(category).key(key).value(key)
                .sortOrder(order).active(true)
                .build();
    }
}
