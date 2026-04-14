package com.ces.erp.common.seeder;

import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.enums.RiskLevel;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class GarageSeeder implements CommandLineRunner {

    private final EquipmentRepository equipmentRepository;
    private final ContractorRepository contractorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (equipmentRepository.count() > 0) return;
        log.info("Qaraj seed edilir...");

        // ─── Podratçılar ──────────────────────────────────────────────────────
        Contractor c1 = contractorRepository.save(Contractor.builder()
                .companyName("İnşaat Texnikası MMC")
                .voen("1234567890")
                .contactPerson("Elçin Həsənov")
                .phone("+994501234567")
                .address("Bakı, Neftçilər pr. 12")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .rating(new BigDecimal("4.30"))
                .notes("Uzunmüddətli tərəfdaş. Texnika icarəsi üzrə əsas podratçı.")
                .build());

        Contractor c2 = contractorRepository.save(Contractor.builder()
                .companyName("TechBuild ASC")
                .voen("9876543210")
                .contactPerson("Rauf Əliyev")
                .phone("+994702345678")
                .address("Bakı, Hüsü Hacıyev 5")
                .paymentType("CASH,TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.MEDIUM)
                .rating(new BigDecimal("3.80"))
                .notes("İri texnikalar üzrə ixtisaslaşıb. Ara-sıra çatdırılma gecikir.")
                .build());

        Contractor c3 = contractorRepository.save(Contractor.builder()
                .companyName("Əziz Texnika İcarə MMC")
                .voen("5544332211")
                .contactPerson("Fərid Əzizov")
                .phone("+994553344556")
                .address("Sumqayıt, Sənaye zonası, kvartal 3")
                .paymentType("CASH")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .rating(new BigDecimal("4.10"))
                .notes("Sumqayıt bölgəsini əhatə edir. Kompressor və generatorlar.")
                .build());

        Contractor c4 = contractorRepository.save(Contractor.builder()
                .companyName("NordTex Servis QSC")
                .voen("6655443322")
                .contactPerson("Natiq Nəsirov")
                .phone("+994704455667")
                .address("Bakı, Pirəkəşkül, sənaye məntəqəsi")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .rating(new BigDecimal("4.50"))
                .notes("Premium texnika servis xidməti. Yüksək keyfiyyət standartları.")
                .build());

        Contractor c5 = contractorRepository.save(Contractor.builder()
                .companyName("Fəhlə Qüvvəsi MMC")
                .voen("7766554433")
                .contactPerson("Orxan Bayramov")
                .phone("+994505566778")
                .address("Gəncə, Kəpəz r., sənaye küç. 18")
                .paymentType("CASH")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.HIGH)
                .rating(new BigDecimal("2.10"))
                .notes("Fəaliyyəti dayandırılıb. Müqavilə ləğv edilib (yanvar 2026).")
                .build());

        log.info("5 podratçı əlavə edildi.");

        // ─── Şirkətin öz texnikaları ──────────────────────────────────────────
        List<Equipment> equipment = List.of(

                // EQ-001 — hazırda aktiv layihədə (RENTED)
                Equipment.builder()
                        .equipmentCode("EQ-001")
                        .name("Hidravlik Ekskavator")
                        .type("Ekskavator")
                        .brand("Caterpillar")
                        .model("320D")
                        .serialNumber("CAT-320D-001")
                        .manufactureYear(2019)
                        .purchaseDate(LocalDate.of(2020, 3, 15))
                        .purchasePrice(new BigDecimal("320000.00"))
                        .currentMarketValue(new BigDecimal("275000.00"))
                        .depreciationRate(new BigDecimal("8.00"))
                        .hourKmCounter(new BigDecimal("5820.00"))
                        .storageLocation("Aktiv sahə — Bakı, Binəqədi")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 11, 10))
                        .nextInspectionDate(LocalDate.of(2026, 5, 10))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.RENTED)
                        .repairStatus("Hazır")
                        .notes("Mart 2026-da Azər İnşaat MMC layihəsindədir.")
                        .build(),

                // EQ-002 — hazırda aktiv layihədə (RENTED)
                Equipment.builder()
                        .equipmentCode("EQ-002")
                        .name("Buldozer")
                        .type("Buldozer")
                        .brand("Komatsu")
                        .model("D65EX-17")
                        .serialNumber("KOM-D65-002")
                        .manufactureYear(2018)
                        .purchaseDate(LocalDate.of(2018, 7, 20))
                        .purchasePrice(new BigDecimal("250000.00"))
                        .currentMarketValue(new BigDecimal("188000.00"))
                        .depreciationRate(new BigDecimal("10.00"))
                        .hourKmCounter(new BigDecimal("8640.00"))
                        .storageLocation("Aktiv sahə — Gəncə")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 10, 5))
                        .nextInspectionDate(LocalDate.of(2026, 4, 5))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.RENTED)
                        .repairStatus("Hazır")
                        .notes("Fevral-mart aylarında Kəpəz Yol Tikintisi layihəsindədir.")
                        .build(),

                // EQ-003 — hazırda aktiv layihədə (RENTED)
                Equipment.builder()
                        .equipmentCode("EQ-003")
                        .name("Mobil Yük Kranı")
                        .type("Kran")
                        .brand("Liebherr")
                        .model("LTM 1070-4.2")
                        .serialNumber("LIE-LTM-003")
                        .manufactureYear(2021)
                        .purchaseDate(LocalDate.of(2021, 5, 10))
                        .purchasePrice(new BigDecimal("890000.00"))
                        .currentMarketValue(new BigDecimal("810000.00"))
                        .depreciationRate(new BigDecimal("5.00"))
                        .hourKmCounter(new BigDecimal("2340.00"))
                        .storageLocation("Aktiv sahə — Bakı Metro")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 12, 1))
                        .nextInspectionDate(LocalDate.of(2026, 6, 1))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.RENTED)
                        .repairStatus("Hazır")
                        .notes("Bakı Metro MMC layihəsindədir. Mart 15-ə qədər.")
                        .build(),

                // EQ-004 — boş, hazır
                Equipment.builder()
                        .equipmentCode("EQ-004")
                        .name("Beton Mikser")
                        .type("Betonqarışdıran")
                        .brand("Mercedes-Benz")
                        .model("Actros 3241 B")
                        .serialNumber("MB-ACT-004")
                        .manufactureYear(2020)
                        .purchaseDate(LocalDate.of(2020, 9, 5))
                        .purchasePrice(new BigDecimal("145000.00"))
                        .currentMarketValue(new BigDecimal("115000.00"))
                        .depreciationRate(new BigDecimal("9.00"))
                        .hourKmCounter(new BigDecimal("97500.00"))
                        .storageLocation("Anbar 1 — Bakı, Sabunçu")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 9, 20))
                        .nextInspectionDate(LocalDate.of(2026, 3, 20))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Fevral layihəsindən qayıtdı. Texniki baxış keçirilib.")
                        .build(),

                // EQ-005 — təmirdədir
                Equipment.builder()
                        .equipmentCode("EQ-005")
                        .name("Vibrasyonlu Kompaktor")
                        .type("Roller / Silindir")
                        .brand("Dynapac")
                        .model("CA2500D")
                        .serialNumber("DYN-CA2500-005")
                        .manufactureYear(2017)
                        .purchaseDate(LocalDate.of(2017, 4, 12))
                        .purchasePrice(new BigDecimal("95000.00"))
                        .currentMarketValue(new BigDecimal("52000.00"))
                        .depreciationRate(new BigDecimal("12.00"))
                        .hourKmCounter(new BigDecimal("11200.00"))
                        .storageLocation("Texniki Xidmət anbarı — Bakı")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 8, 10))
                        .nextInspectionDate(LocalDate.of(2026, 8, 10))
                        .technicalReadinessStatus("Qismən hazır")
                        .status(EquipmentStatus.OUT_OF_SERVICE)
                        .repairStatus("Təmirdədir")
                        .notes("Mühərrik bloku dəyişilir. Mart sonu hazır olacaq.")
                        .build(),

                // EQ-006 — boş, hazır
                Equipment.builder()
                        .equipmentCode("EQ-006")
                        .name("Yük Maşını")
                        .type("Yük maşını")
                        .brand("Volvo")
                        .model("FH 460 6×4")
                        .serialNumber("VOL-FH460-006")
                        .manufactureYear(2022)
                        .purchaseDate(LocalDate.of(2022, 2, 14))
                        .purchasePrice(new BigDecimal("175000.00"))
                        .currentMarketValue(new BigDecimal("160000.00"))
                        .depreciationRate(new BigDecimal("6.50"))
                        .hourKmCounter(new BigDecimal("128000.00"))
                        .storageLocation("Anbar 1 — Bakı, Sabunçu")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2026, 1, 15))
                        .nextInspectionDate(LocalDate.of(2026, 7, 15))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Material daşınması üçün. Son TÜV yanvar 2026.")
                        .build(),

                // EQ-007 — boş, hazır
                Equipment.builder()
                        .equipmentCode("EQ-007")
                        .name("Greyder")
                        .type("Greyder")
                        .brand("Caterpillar")
                        .model("140M3")
                        .serialNumber("CAT-140M-007")
                        .manufactureYear(2020)
                        .purchaseDate(LocalDate.of(2020, 6, 8))
                        .purchasePrice(new BigDecimal("280000.00"))
                        .currentMarketValue(new BigDecimal("235000.00"))
                        .depreciationRate(new BigDecimal("8.50"))
                        .hourKmCounter(new BigDecimal("4100.00"))
                        .storageLocation("Anbar 2 — Sumqayıt")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 11, 20))
                        .nextInspectionDate(LocalDate.of(2026, 5, 20))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Yol hamarlanması üçün. Gəncə bölgəsindən qayıtdı (fevral).")
                        .build(),

                // EQ-008 — boş, hazır
                Equipment.builder()
                        .equipmentCode("EQ-008")
                        .name("Hidravlik Perforator")
                        .type("Qazıcı")
                        .brand("Doosan")
                        .model("DX140LCR-5")
                        .serialNumber("DOO-DX140-008")
                        .manufactureYear(2021)
                        .purchaseDate(LocalDate.of(2021, 8, 30))
                        .purchasePrice(new BigDecimal("210000.00"))
                        .currentMarketValue(new BigDecimal("190000.00"))
                        .depreciationRate(new BigDecimal("7.00"))
                        .hourKmCounter(new BigDecimal("2850.00"))
                        .storageLocation("Anbar 1 — Bakı, Sabunçu")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 12, 10))
                        .nextInspectionDate(LocalDate.of(2026, 6, 10))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Güclü qayalı torpaqlar üçün.")
                        .build(),

                // ─── İnvestor texnikaları ──────────────────────────────────────

                // EQ-009 — boş (yanvar layihəsindən qayıtdı)
                Equipment.builder()
                        .equipmentCode("EQ-009")
                        .name("Mini Ekskavator")
                        .type("Mini Ekskavator")
                        .brand("Bobcat")
                        .model("E50")
                        .serialNumber("BOB-E50-009")
                        .manufactureYear(2022)
                        .purchaseDate(LocalDate.of(2022, 11, 8))
                        .purchasePrice(new BigDecimal("82000.00"))
                        .currentMarketValue(new BigDecimal("75000.00"))
                        .depreciationRate(new BigDecimal("6.00"))
                        .hourKmCounter(new BigDecimal("1340.00"))
                        .storageLocation("Anbar 2 — Sumqayıt")
                        .ownershipType(OwnershipType.INVESTOR)
                        .ownerInvestorName("Tural Əliyev (Azərbaycan İnvestisiya Holdinqi)")
                        .ownerInvestorVoen("1122334455")
                        .ownerInvestorPhone("+994501112233")
                        .lastInspectionDate(LocalDate.of(2026, 1, 20))
                        .nextInspectionDate(LocalDate.of(2026, 7, 20))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("İnvestor texnikası. Yanvar SkyLine layihəsindən qayıtdı.")
                        .build(),

                // EQ-010 — boş, hazır
                Equipment.builder()
                        .equipmentCode("EQ-010")
                        .name("Teleskopik Yükləyici")
                        .type("Teleskopik Yükləyici")
                        .brand("JCB")
                        .model("535-140")
                        .serialNumber("JCB-535-010")
                        .manufactureYear(2021)
                        .purchaseDate(LocalDate.of(2021, 3, 22))
                        .purchasePrice(new BigDecimal("138000.00"))
                        .currentMarketValue(new BigDecimal("122000.00"))
                        .depreciationRate(new BigDecimal("7.00"))
                        .hourKmCounter(new BigDecimal("2680.00"))
                        .storageLocation("Anbar 3 — Abşeron")
                        .ownershipType(OwnershipType.INVESTOR)
                        .ownerInvestorName("Aynur Həsənova (Silk Road Capital MMC)")
                        .ownerInvestorVoen("2233445566")
                        .ownerInvestorPhone("+994702223344")
                        .lastInspectionDate(LocalDate.of(2025, 12, 15))
                        .nextInspectionDate(LocalDate.of(2026, 6, 15))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("İnvestor texnikası. AzərGold layihəsindən qayıtdı (fevral 18).")
                        .build(),

                // ─── Podratçı texnikaları ──────────────────────────────────────

                // EQ-011 — podratçı (İnşaat Texnikası MMC), boş
                Equipment.builder()
                        .equipmentCode("EQ-011")
                        .name("Asfalt Silindir")
                        .type("Roller / Silindir")
                        .brand("Bomag")
                        .model("BW 213 D-5")
                        .serialNumber("BOM-BW213-011")
                        .manufactureYear(2020)
                        .purchaseDate(LocalDate.of(2020, 6, 18))
                        .purchasePrice(new BigDecimal("110000.00"))
                        .currentMarketValue(new BigDecimal("90000.00"))
                        .depreciationRate(new BigDecimal("9.00"))
                        .hourKmCounter(new BigDecimal("3850.00"))
                        .storageLocation("Podratçı anbarı — İnşaat Texnikası MMC")
                        .ownershipType(OwnershipType.CONTRACTOR)
                        .ownerContractor(c1)
                        .lastInspectionDate(LocalDate.of(2025, 10, 5))
                        .nextInspectionDate(LocalDate.of(2026, 4, 5))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("İnşaat Texnikası MMC-yə məxsusdur.")
                        .build(),

                // EQ-012 — podratçı (TechBuild ASC), boş
                Equipment.builder()
                        .equipmentCode("EQ-012")
                        .name("Ekskavator-Yükləyici")
                        .type("Ekskavator")
                        .brand("JCB")
                        .model("3CX Pro")
                        .serialNumber("JCB-3CX-012")
                        .manufactureYear(2019)
                        .purchaseDate(LocalDate.of(2019, 8, 30))
                        .purchasePrice(new BigDecimal("88000.00"))
                        .currentMarketValue(new BigDecimal("64000.00"))
                        .depreciationRate(new BigDecimal("11.00"))
                        .hourKmCounter(new BigDecimal("7200.00"))
                        .storageLocation("TechBuild ASC anbarı")
                        .ownershipType(OwnershipType.CONTRACTOR)
                        .ownerContractor(c2)
                        .lastInspectionDate(LocalDate.of(2025, 9, 1))
                        .nextInspectionDate(LocalDate.of(2026, 3, 1))
                        .technicalReadinessStatus("Qismən hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("TechBuild ASC-yə məxsusdur. SkyLine layihəsindən qayıtdı.")
                        .build(),

                // EQ-013 — podratçı (Əziz Texnika), boş
                Equipment.builder()
                        .equipmentCode("EQ-013")
                        .name("Hava Kompressoru")
                        .type("Kompressor")
                        .brand("Doosan")
                        .model("Portable HP 375")
                        .serialNumber("DOO-HP375-013")
                        .manufactureYear(2021)
                        .purchaseDate(LocalDate.of(2021, 7, 14))
                        .purchasePrice(new BigDecimal("45000.00"))
                        .currentMarketValue(new BigDecimal("39000.00"))
                        .depreciationRate(new BigDecimal("8.00"))
                        .hourKmCounter(new BigDecimal("1950.00"))
                        .storageLocation("Əziz Texnika anbarı — Sumqayıt")
                        .ownershipType(OwnershipType.CONTRACTOR)
                        .ownerContractor(c3)
                        .lastInspectionDate(LocalDate.of(2026, 1, 8))
                        .nextInspectionDate(LocalDate.of(2026, 7, 8))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Əziz Texnika İcarə MMC-yə məxsusdur.")
                        .build(),

                // EQ-014 — podratçı (NordTex), boş
                Equipment.builder()
                        .equipmentCode("EQ-014")
                        .name("Dizel Generator")
                        .type("Generator")
                        .brand("Caterpillar")
                        .model("C9 275 kVA")
                        .serialNumber("CAT-C9-014")
                        .manufactureYear(2020)
                        .purchaseDate(LocalDate.of(2020, 4, 22))
                        .purchasePrice(new BigDecimal("68000.00"))
                        .currentMarketValue(new BigDecimal("58000.00"))
                        .depreciationRate(new BigDecimal("8.50"))
                        .hourKmCounter(new BigDecimal("4200.00"))
                        .storageLocation("NordTex Servis anbarı — Pirəkəşkül")
                        .ownershipType(OwnershipType.CONTRACTOR)
                        .ownerContractor(c4)
                        .lastInspectionDate(LocalDate.of(2025, 12, 20))
                        .nextInspectionDate(LocalDate.of(2026, 6, 20))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("NordTex Servis QSC-yə məxsusdur. Elektrik kəsinti anlarında.")
                        .build()
        );

        equipmentRepository.saveAll(equipment);
        log.info("{} texnika əlavə edildi.", equipment.size());
    }
}
