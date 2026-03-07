package com.ces.erp.common.seeder;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.ProjectStatus;
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
import java.util.List;

@Component
@Order(8)
@RequiredArgsConstructor
@Slf4j
public class AccountingSeeder implements CommandLineRunner {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final ContractorRepository contractorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (invoiceRepository.count() > 0) return;
        log.info("Mühasibatlıq qaimələri seed edilir...");

        List<Project> completedProjects = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED && !p.isDeleted())
                .toList();

        List<Project> activeProjects = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE && !p.isDeleted())
                .toList();

        List<Contractor> contractors = contractorRepository.findAllByDeletedFalse();
        Contractor contractor = contractors.isEmpty() ? null : contractors.get(0);

        // ─── A — Gəlir qaimələri ──────────────────────────────────────────────

        if (!completedProjects.isEmpty()) {
            Project cp = completedProjects.get(0);
            invoiceRepository.save(Invoice.builder()
                    .type(InvoiceType.INCOME)
                    .etaxesId("ETX-2026-00142")
                    .invoiceNumber("Q-2026-0001")
                    .amount(new BigDecimal("2000.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 16))
                    .companyName(cp.getRequest() != null ? cp.getRequest().getCompanyName() : "Müştəri")
                    .equipmentName(cp.getRequest() != null && cp.getRequest().getSelectedEquipment() != null
                            ? cp.getRequest().getSelectedEquipment().getName() : "Ekskavator")
                    .project(cp)
                    .notes("Bağlanmış layihə üzrə texnika icarəsi gəliri")
                    .build());
            log.info("A qaiməsi yaradıldı: ETX-2026-00142");
        }

        if (!activeProjects.isEmpty()) {
            Project ap = activeProjects.get(0);
            invoiceRepository.save(Invoice.builder()
                    .type(InvoiceType.INCOME)
                    .etaxesId("ETX-2026-00198")
                    .invoiceNumber("Q-2026-0002")
                    .amount(new BigDecimal("2550.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 21))
                    .companyName(ap.getRequest() != null ? ap.getRequest().getCompanyName() : "Müştəri")
                    .equipmentName("Hidravlik Ekskavator")
                    .project(ap)
                    .notes("Aktiv layihə üzrə avans qaimə")
                    .build());
            log.info("A qaiməsi yaradıldı: ETX-2026-00198");
        }

        // ─── B1 — Podratçı qaimələri ──────────────────────────────────────────

        if (contractor != null && !completedProjects.isEmpty()) {
            invoiceRepository.save(Invoice.builder()
                    .type(InvoiceType.CONTRACTOR_EXPENSE)
                    .invoiceNumber("POD-2026-055")
                    .amount(new BigDecimal("680.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 14))
                    .equipmentName("Texnika icarəsi xidməti")
                    .contractor(contractor)
                    .project(completedProjects.get(0))
                    .notes("Podratçının texnika xidmət haqqı")
                    .build());
            log.info("B1 qaiməsi yaradıldı: POD-2026-055 → {}", contractor.getCompanyName());
        }

        if (contractor != null && !activeProjects.isEmpty()) {
            invoiceRepository.save(Invoice.builder()
                    .type(InvoiceType.CONTRACTOR_EXPENSE)
                    .invoiceNumber("POD-2026-061")
                    .amount(new BigDecimal("420.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 22))
                    .equipmentName("Operator xidməti")
                    .contractor(contractor)
                    .project(activeProjects.get(0))
                    .notes("Aktiv layihə — podratçı operator ödənişi")
                    .build());
            log.info("B1 qaiməsi yaradıldı: POD-2026-061");
        }

        // ─── B2 — Şirkət xərcləri ─────────────────────────────────────────────

        invoiceRepository.save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE)
                .invoiceNumber("SRV-2026-012")
                .amount(new BigDecimal("315.00"))
                .invoiceDate(LocalDate.of(2026, 2, 10))
                .companyName("AutoServis MMC")
                .serviceDescription("Texniki baxış və yağ dəyişimi — EQ-001")
                .project(!completedProjects.isEmpty() ? completedProjects.get(0) : null)
                .notes("Ekskavator texniki xidməti")
                .build());
        log.info("B2 qaiməsi yaradıldı: SRV-2026-012 → AutoServis MMC");

        invoiceRepository.save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE)
                .invoiceNumber("SRV-2026-019")
                .amount(new BigDecimal("180.00"))
                .invoiceDate(LocalDate.of(2026, 2, 24))
                .companyName("Loqistik ASC")
                .serviceDescription("Texnikanın daşınması xidməti")
                .project(!activeProjects.isEmpty() ? activeProjects.get(0) : null)
                .notes("Aktiv layihəyə texnika çatdırılması")
                .build());
        log.info("B2 qaiməsi yaradıldı: SRV-2026-019 → Loqistik ASC");

        invoiceRepository.save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE)
                .invoiceNumber(null)
                .amount(new BigDecimal("95.00"))
                .invoiceDate(LocalDate.of(2026, 3, 1))
                .companyName("Yanacaq Şirkəti")
                .serviceDescription("Yanacaq (dizel) — 50L")
                .project(null)
                .notes("Ümumi şirkət yanacaq xərci, qaimə nömrəsi gözlənilir")
                .build());
        log.info("B2 qaiməsi yaradıldı: ümumi yanacaq xərci");

        log.info("Mühasibatlıq seederi tamamlandı. Cəmi {} qaimə.", invoiceRepository.count());
    }
}
