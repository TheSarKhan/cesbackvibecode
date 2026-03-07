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
@Order(2)
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

        // ─── Podratçılar ───────────────────────────────────────────────────────
        Contractor c1 = seedContractor(
                "İnşaat Texnikası MMC", "1234567890", "Elçin Həsənov", "+994501234567",
                "Bakı, Neftçilər pr. 12", ContractorStatus.ACTIVE, RiskLevel.LOW
        );
        Contractor c2 = seedContractor(
                "TechBuild ASC", "9876543210", "Rauf Əliyev", "+994702345678",
                "Bakı, Hüsü Hacıyev 5", ContractorStatus.ACTIVE, RiskLevel.MEDIUM
        );

        // ─── Şirkət texnikaları ────────────────────────────────────────────────
        List<Equipment> equipment = List.of(

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
                        .currentMarketValue(new BigDecimal("280000.00"))
                        .depreciationRate(new BigDecimal("8.00"))
                        .hourKmCounter(new BigDecimal("4500.00"))
                        .storageLocation("Sahə 1 – Bakı")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 10, 1))
                        .nextInspectionDate(LocalDate.of(2026, 4, 1))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Əsas sahə ekskavatorudur")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-002")
                        .name("Buldozer")
                        .type("Buldozer")
                        .brand("Komatsu")
                        .model("D65EX")
                        .serialNumber("KOM-D65-002")
                        .manufactureYear(2018)
                        .purchaseDate(LocalDate.of(2018, 7, 20))
                        .purchasePrice(new BigDecimal("250000.00"))
                        .currentMarketValue(new BigDecimal("195000.00"))
                        .depreciationRate(new BigDecimal("10.00"))
                        .hourKmCounter(new BigDecimal("7200.00"))
                        .storageLocation("Sahə 1 – Bakı")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 9, 15))
                        .nextInspectionDate(LocalDate.of(2026, 3, 15))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.RENTED)
                        .repairStatus("Hazır")
                        .notes("Layihə 3-də istifadə olunur")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-003")
                        .name("Yük Kranı")
                        .type("Kran")
                        .brand("Liebherr")
                        .model("LTM 1070")
                        .serialNumber("LIE-LTM-003")
                        .manufactureYear(2021)
                        .purchaseDate(LocalDate.of(2021, 5, 10))
                        .purchasePrice(new BigDecimal("890000.00"))
                        .currentMarketValue(new BigDecimal("820000.00"))
                        .depreciationRate(new BigDecimal("5.00"))
                        .hourKmCounter(new BigDecimal("1800.00"))
                        .storageLocation("Anbar 2 – Sumqayıt")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 11, 1))
                        .nextInspectionDate(LocalDate.of(2026, 5, 1))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Ağır yük qaldırma kranidir")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-004")
                        .name("Beton Mikser")
                        .type("Mikser")
                        .brand("Mercedes-Benz")
                        .model("Actros 3241")
                        .serialNumber("MB-ACT-004")
                        .manufactureYear(2020)
                        .purchaseDate(LocalDate.of(2020, 9, 5))
                        .purchasePrice(new BigDecimal("145000.00"))
                        .currentMarketValue(new BigDecimal("118000.00"))
                        .depreciationRate(new BigDecimal("9.00"))
                        .hourKmCounter(new BigDecimal("85000.00"))
                        .storageLocation("Anbar 1 – Bakı")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 8, 20))
                        .nextInspectionDate(LocalDate.of(2026, 2, 20))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-005")
                        .name("Kompaktor")
                        .type("Kompaktor")
                        .brand("Dynapac")
                        .model("CA2500D")
                        .serialNumber("DYN-CA2500-005")
                        .manufactureYear(2017)
                        .purchaseDate(LocalDate.of(2017, 4, 12))
                        .purchasePrice(new BigDecimal("95000.00"))
                        .currentMarketValue(new BigDecimal("58000.00"))
                        .depreciationRate(new BigDecimal("12.00"))
                        .hourKmCounter(new BigDecimal("9800.00"))
                        .storageLocation("Sahə 2 – Gəncə")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2025, 7, 10))
                        .nextInspectionDate(LocalDate.of(2026, 1, 10))
                        .technicalReadinessStatus("Qismən hazır")
                        .status(EquipmentStatus.OUT_OF_SERVICE)
                        .repairStatus("Təmirdədir")
                        .notes("Mühərrik problemi var, təmirdədir")
                        .build(),

                // ─── İnvestor texnikası ───────────────────────────────────────

                Equipment.builder()
                        .equipmentCode("EQ-006")
                        .name("Mini Ekskavator")
                        .type("Ekskavator")
                        .brand("Bobcat")
                        .model("E35")
                        .serialNumber("BOB-E35-006")
                        .manufactureYear(2022)
                        .purchaseDate(LocalDate.of(2022, 11, 8))
                        .purchasePrice(new BigDecimal("78000.00"))
                        .currentMarketValue(new BigDecimal("72000.00"))
                        .depreciationRate(new BigDecimal("6.00"))
                        .hourKmCounter(new BigDecimal("950.00"))
                        .storageLocation("Sahə 1 – Bakı")
                        .ownershipType(OwnershipType.INVESTOR)
                        .ownerInvestorName("Natiq Məmmədov")
                        .ownerInvestorVoen("5544332211")
                        .ownerInvestorPhone("+994553456789")
                        .lastInspectionDate(LocalDate.of(2025, 12, 1))
                        .nextInspectionDate(LocalDate.of(2026, 6, 1))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("İnvestor tərəfindən verilmiş texnika")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-007")
                        .name("Teleskopik Yükləyici")
                        .type("Yükləyici")
                        .brand("JCB")
                        .model("533-105")
                        .serialNumber("JCB-533-007")
                        .manufactureYear(2021)
                        .purchaseDate(LocalDate.of(2021, 3, 22))
                        .purchasePrice(new BigDecimal("135000.00"))
                        .currentMarketValue(new BigDecimal("120000.00"))
                        .depreciationRate(new BigDecimal("7.00"))
                        .hourKmCounter(new BigDecimal("2300.00"))
                        .storageLocation("Anbar 3 – Abşeron")
                        .ownershipType(OwnershipType.INVESTOR)
                        .ownerInvestorName("Günel Hüseynova")
                        .ownerInvestorVoen("6677889900")
                        .ownerInvestorPhone("+994704567890")
                        .lastInspectionDate(LocalDate.of(2025, 10, 15))
                        .nextInspectionDate(LocalDate.of(2026, 4, 15))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.RENTED)
                        .repairStatus("Hazır")
                        .notes("")
                        .build(),

                // ─── Podratçı texnikaları ─────────────────────────────────────

                Equipment.builder()
                        .equipmentCode("EQ-008")
                        .name("Asfalt Silindir")
                        .type("Silindir")
                        .brand("Bomag")
                        .model("BW 213 D-5")
                        .serialNumber("BOM-BW213-008")
                        .manufactureYear(2020)
                        .purchaseDate(LocalDate.of(2020, 6, 18))
                        .purchasePrice(new BigDecimal("110000.00"))
                        .currentMarketValue(new BigDecimal("92000.00"))
                        .depreciationRate(new BigDecimal("9.00"))
                        .hourKmCounter(new BigDecimal("3100.00"))
                        .storageLocation("Podratçı sahəsi")
                        .ownershipType(OwnershipType.CONTRACTOR)
                        .ownerContractor(c1)
                        .lastInspectionDate(LocalDate.of(2025, 9, 5))
                        .nextInspectionDate(LocalDate.of(2026, 3, 5))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("İnşaat Texnikası MMC-yə məxsusdur")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-009")
                        .name("Ekskavator-Yükləyici")
                        .type("Ekskavator")
                        .brand("JCB")
                        .model("3CX")
                        .serialNumber("JCB-3CX-009")
                        .manufactureYear(2019)
                        .purchaseDate(LocalDate.of(2019, 8, 30))
                        .purchasePrice(new BigDecimal("88000.00"))
                        .currentMarketValue(new BigDecimal("65000.00"))
                        .depreciationRate(new BigDecimal("11.00"))
                        .hourKmCounter(new BigDecimal("6500.00"))
                        .storageLocation("TechBuild anbarı")
                        .ownershipType(OwnershipType.CONTRACTOR)
                        .ownerContractor(c2)
                        .lastInspectionDate(LocalDate.of(2025, 8, 1))
                        .nextInspectionDate(LocalDate.of(2026, 2, 1))
                        .technicalReadinessStatus("Qismən hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("TechBuild ASC-yə məxsusdur")
                        .build(),

                Equipment.builder()
                        .equipmentCode("EQ-010")
                        .name("Yük Maşını")
                        .type("Nəqliyyat")
                        .brand("Volvo")
                        .model("FH 460")
                        .serialNumber("VOL-FH460-010")
                        .manufactureYear(2022)
                        .purchaseDate(LocalDate.of(2022, 2, 14))
                        .purchasePrice(new BigDecimal("175000.00"))
                        .currentMarketValue(new BigDecimal("162000.00"))
                        .depreciationRate(new BigDecimal("6.50"))
                        .hourKmCounter(new BigDecimal("112000.00"))
                        .storageLocation("Anbar 1 – Bakı")
                        .ownershipType(OwnershipType.COMPANY)
                        .lastInspectionDate(LocalDate.of(2026, 1, 10))
                        .nextInspectionDate(LocalDate.of(2026, 7, 10))
                        .technicalReadinessStatus("Hazır")
                        .status(EquipmentStatus.AVAILABLE)
                        .repairStatus("Hazır")
                        .notes("Materialların daşınması üçün istifadə olunur")
                        .build()
        );

        equipmentRepository.saveAll(equipment);
        log.info("{} texnika əlavə edildi.", equipment.size());
    }

    private Contractor seedContractor(String name, String voen, String contact, String phone,
                                       String address, ContractorStatus status, RiskLevel risk) {
        return contractorRepository.findAll().stream()
                .filter(c -> c.getVoen().equals(voen))
                .findFirst()
                .orElseGet(() -> contractorRepository.save(
                        Contractor.builder()
                                .companyName(name)
                                .voen(voen)
                                .contactPerson(contact)
                                .phone(phone)
                                .address(address)
                                .status(status)
                                .riskLevel(risk)
                                .rating(BigDecimal.ZERO)
                                .build()
                ));
    }
}
