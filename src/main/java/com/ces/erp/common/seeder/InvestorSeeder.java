package com.ces.erp.common.seeder;

import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * İnvestorlar üçün hazırkı mərhələdə real mənbə (Excel/kataloq) verilməyib.
 * Test/demo investor məlumatı seed edilmir — real investorlar admin panelindən əlavə olunmalıdır.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class InvestorSeeder implements CommandLineRunner {

    private final InvestorRepository investorRepository;

    @Override
    public void run(String... args) {
        log.info("İnvestor seed mənbəyi yoxdur — {} investor bazada mövcuddur.", investorRepository.count());
    }
}
