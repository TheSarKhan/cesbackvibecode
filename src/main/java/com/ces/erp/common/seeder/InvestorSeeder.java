package com.ces.erp.common.seeder;

import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mənbə: "Operatorlar.xlsx" → "Лист1" vərəqi ("TEXNİKALARIN CARİ DURUMU" hesabatı, Stok sütunu).
 * MEMO MMC və Translift-in VÖEN-ləri Podratçı Bazasındakı eyni şirkətlərlə üst-üstə düşür (real VÖEN istifadə olunur).
 * Ceyran Səmədova və Azər Seyidov fiziki şəxsdir, heç bir mənbədə VÖEN-ləri yoxdur —
 * Investor.voen NOT NULL/UNIQUE olduğu üçün "VOEN yoxdur - <ad>" placeholder istifadə olunur.
 * Texnikaları GarageSeeder-də ownershipType=INVESTOR ilə əlaqələndirilib.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class InvestorSeeder implements CommandLineRunner {

    private final InvestorRepository investorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (investorRepository.count() > 0) return;
        log.info("İnvestorlar seed edilir...");

        List<Investor> investors = List.of(

                Investor.builder()
                        .companyName("Ceyran Səmədova")
                        .voen("VOEN-YOXDUR-CS")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Fiziki şəxs, VÖEN mövcud deyil. Qarajda 3 texnikası var.")
                        .build(),

                Investor.builder()
                        .companyName("MEMO MMC")
                        .voen("1501310751")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Qarajda 3 texnikası var.")
                        .build(),

                Investor.builder()
                        .companyName("Azər Seyidov")
                        .voen("VOEN-YOXDUR-AS")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Fiziki şəxs, VÖEN mövcud deyil. Qarajda 4 texnikası var.")
                        .build(),

                Investor.builder()
                        .companyName("Translift")
                        .voen("2006037141")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Qarajda 2 texnikası var.")
                        .build()
        );

        investorRepository.saveAll(investors);
        log.info("{} investor əlavə edildi.", investors.size());
    }
}
