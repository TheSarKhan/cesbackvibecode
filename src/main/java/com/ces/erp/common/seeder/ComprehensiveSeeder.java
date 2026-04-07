package com.ces.erp.common.seeder;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Order(11)
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveSeeder implements CommandLineRunner {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Yalnız əgər test data yoxdursa seed et
        if (invoiceRepository.count() > 100) return;

        log.info("Tamamlayıcı test datası seed edilir...");

        // Mövcud proyektləri al
        var projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            log.warn("Proyekt tapılmadı, test datası skip edilir");
            return;
        }

        // DRAFT və SENT qaiməsi ilə test layihələr yarat
        projects.stream().limit(5).forEach(project -> {
            createTestInvoices(project);
        });

        log.info("Seeder tamamlandı. Cəmi {} qaimə.", invoiceRepository.count());
    }

    private void createTestInvoices(Project project) {
        if (project == null || project.getRequest() == null) return;

        String companyName = project.getRequest().getCompanyName();

        // DRAFT qaime
        save(Invoice.builder()
                .type(InvoiceType.INCOME)
                .status(InvoiceStatus.DRAFT)
                .amount(new BigDecimal("5000.00"))
                .invoiceDate(LocalDate.now().minusDays(5))
                .companyName(companyName)
                .equipmentName("Test Texnika")
                .project(project)
                .notes("Test DRAFT qaime - gonderilmemis")
                .periodMonth(LocalDate.now().getMonthValue())
                .periodYear(LocalDate.now().getYear())
                .monthlyRate(new BigDecimal("5000"))
                .workingDaysInMonth(26)
                .workingHoursPerDay(9)
                .overtimeRate(new BigDecimal("1.0"))
                .build());

        // SENT qaime (2-3 gun once)
        save(Invoice.builder()
                .type(InvoiceType.INCOME)
                .status(InvoiceStatus.SENT)
                .invoiceNumber("TEST-" + System.currentTimeMillis() % 1000)
                .amount(new BigDecimal("7500.00"))
                .invoiceDate(LocalDate.now().minusDays(10))
                .etaxesId("ETX-TEST-" + System.currentTimeMillis() % 10000)
                .companyName(companyName)
                .equipmentName("Test Texnika SENT")
                .project(project)
                .notes("Test SENT qaime - muhasibatliga gonderilmis")
                .periodMonth(LocalDate.now().minusMonths(1).getMonthValue())
                .periodYear(LocalDate.now().getYear())
                .monthlyRate(new BigDecimal("7500"))
                .workingDaysInMonth(26)
                .workingHoursPerDay(9)
                .overtimeRate(new BigDecimal("1.0"))
                .build());
    }

    private void save(Invoice invoice) {
        try {
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.warn("Qaime save edilə bilmədi: {}", e.getMessage());
        }
    }
}
