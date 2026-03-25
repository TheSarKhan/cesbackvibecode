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

import java.math.BigDecimal;
import java.util.List;

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
                        .companyName("Azərbaycan İnvestisiya Holdinqi")
                        .voen("1122334455")
                        .contactPerson("Tural Əliyev")
                        .contactPhone("+994501112233")
                        .address("Bakı, İstiqlaliyyət küç. 8")
                        .paymentType("Bank köçürməsi")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .rating(new BigDecimal("4.80"))
                        .notes("Əsas strateji investor tərəfdaşı. 3 texnika sahibi.")
                        .build(),

                Investor.builder()
                        .companyName("Silk Road Capital MMC")
                        .voen("2233445566")
                        .contactPerson("Aynur Həsənova")
                        .contactPhone("+994702223344")
                        .address("Bakı, Nizami küç. 34")
                        .paymentType("Bank köçürməsi")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .rating(new BigDecimal("4.50"))
                        .notes("Tikinti layihələrini maliyyələşdirir. 2 texnika sahibi.")
                        .build(),

                Investor.builder()
                        .companyName("Caspian Ventures ASC")
                        .voen("3344556677")
                        .contactPerson("Rəşad Məmmədov")
                        .contactPhone("+994553334455")
                        .address("Bakı, Tbilisi pr. 22")
                        .paymentType("Nağd / Bank")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.MEDIUM)
                        .rating(new BigDecimal("3.90"))
                        .notes("Orta ölçülü layihələr üzrə investisiya. Ödəniş bəzən gecikir.")
                        .build(),

                Investor.builder()
                        .companyName("Gulf Bridge Investment")
                        .voen("4455667788")
                        .contactPerson("Nigar Quliyeva")
                        .contactPhone("+994504445566")
                        .address("Bakı, Xaqani küç. 15")
                        .paymentType("Bank köçürməsi")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .rating(new BigDecimal("4.20"))
                        .notes("Xarici investor, beynəlxalq layihələr üzrə əməkdaşlıq.")
                        .build(),

                Investor.builder()
                        .companyName("AzərEnerjİnvest QSC")
                        .voen("5566778899")
                        .contactPerson("Kənan Nəsirov")
                        .contactPhone("+994705556677")
                        .address("Bakı, Xəzər küç. 41")
                        .paymentType("Bank köçürməsi")
                        .status(ContractorStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .rating(new BigDecimal("4.60"))
                        .notes("Enerji sektoru investoru. Uzunmüddətli müqavilə bazasında.")
                        .build(),

                Investor.builder()
                        .companyName("Baku Real Estate Partners")
                        .voen("6677889900")
                        .contactPerson("Elnur Hüseynov")
                        .contactPhone("+994556667788")
                        .address("Bakı, Rəşid Behbudov küç. 7")
                        .paymentType("Nağd")
                        .status(ContractorStatus.INACTIVE)
                        .riskLevel(RiskLevel.HIGH)
                        .rating(new BigDecimal("2.50"))
                        .notes("Fəaliyyəti müvəqqəti dayandırılıb. Borclu qalıq: 12.400 AZN.")
                        .build()
        );

        investorRepository.saveAll(investors);
        log.info("{} investor əlavə edildi.", investors.size());
    }
}
