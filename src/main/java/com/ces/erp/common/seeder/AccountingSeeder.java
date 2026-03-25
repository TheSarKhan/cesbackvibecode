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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(10)
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

        List<Project> completed = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED && !p.isDeleted())
                .toList();
        List<Project> active = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE && !p.isDeleted())
                .toList();

        Map<String, Project> completedByCode = completed.stream()
                .collect(Collectors.toMap(Project::getProjectCode, Function.identity(), (a, b) -> a));
        Map<String, Project> activeByCode = active.stream()
                .collect(Collectors.toMap(Project::getProjectCode, Function.identity(), (a, b) -> a));

        List<Contractor> contractors = contractorRepository.findAllByDeletedFalse();
        Map<String, Contractor> contByName = contractors.stream()
                .collect(Collectors.toMap(Contractor::getCompanyName, Function.identity(), (a, b) -> a));
        Contractor c1 = contByName.get("İnşaat Texnikası MMC");
        Contractor c2 = contByName.get("TechBuild ASC");
        Contractor c3 = contByName.get("Əziz Texnika İcarə MMC");
        Contractor c4 = contByName.get("NordTex Servis QSC");

        // ── Tamamlanmış layihələrin gəlir qaimələri (A tipli) ─────────────────

        // PRJ-0001: Azər İnşaat — Binəqədi
        Project p1 = completed.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("Binəqədi")).findFirst().orElse(null);
        if (p1 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00071")
                    .invoiceNumber("Q-2026-0001").amount(new BigDecimal("3750.00"))
                    .invoiceDate(LocalDate.of(2026, 1, 21))
                    .companyName("Azər İnşaat MMC").equipmentName("Hidravlik Ekskavator CAT 320D")
                    .project(p1).notes("Binəqədi torpaq qazıma layihəsi. Texnika + operator.").build());
        }

        // PRJ-0002: Grand Build — Sabunçu kran
        Project p2 = completed.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("Sabunçu")).findFirst().orElse(null);
        if (p2 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00089")
                    .invoiceNumber("Q-2026-0002").amount(new BigDecimal("6120.00"))
                    .invoiceDate(LocalDate.of(2026, 1, 23))
                    .companyName("Grand Build ASC").equipmentName("Mobil Yük Kranı Liebherr LTM 1070")
                    .project(p2).notes("Sabunçu bina tikintisi. Kran + operator + daşınma.").build());
        }

        // PRJ-0003: SOCAR — Balaxanı buldozer
        Project p3 = completed.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("Balaxanı")).findFirst().orElse(null);
        if (p3 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00112")
                    .invoiceNumber("Q-2026-0003").amount(new BigDecimal("4940.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 4))
                    .companyName("SOCAR Tikinti ASC").equipmentName("Buldozer Komatsu D65EX")
                    .project(p3).notes("Balaxanı emal sahəsi. Buldozer + operator + daşınma.").build());
        }

        // PRJ-0004: SkyLine — Sumqayıt çimərlik
        Project p4 = completed.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("çimərlik")).findFirst().orElse(null);
        if (p4 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00134")
                    .invoiceNumber("Q-2026-0004").amount(new BigDecimal("2000.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 2))
                    .companyName("SkyLine Tikinti QSC").equipmentName("Ekskavator-Yükləyici JCB 3CX Pro")
                    .project(p4).notes("Sumqayıt çimərlik kompleksi bünövrə işləri.").build());
        }

        // PRJ-0005: AzərGold — Daşkəsən teleskopik
        Project p5 = completed.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("yüklənmə")).findFirst().orElse(null);
        if (p5 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00158")
                    .invoiceNumber("Q-2026-0005").amount(new BigDecimal("4400.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 19))
                    .companyName("AzərGold QSC").equipmentName("Teleskopik Yükləyici JCB 535-140")
                    .project(p5).notes("Daşkəsən mədən sahəsi. Yükləyici + operator + daşınma.").build());
        }

        // ── Aktiv layihələrin avans qaimələri ─────────────────────────────────

        // PRJ-0006: Bakı Metro — avans
        Project p6 = active.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("8 Noyabr")).findFirst().orElse(null);
        if (p6 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00189")
                    .invoiceNumber("Q-2026-0006").amount(new BigDecimal("8200.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 15))
                    .companyName("Bakı Metro MMC").equipmentName("Mobil Yük Kranı Liebherr LTM 1070")
                    .project(p6).notes("8 Noyabr stansiyası — aylıq avans qaiməsi.").build());
        }

        // PRJ-0007: Kəpəz — avans
        Project p7 = active.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("magistral")).findFirst().orElse(null);
        if (p7 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00201")
                    .invoiceNumber("Q-2026-0007").amount(new BigDecimal("3950.00"))
                    .invoiceDate(LocalDate.of(2026, 2, 24))
                    .companyName("Kəpəz Yol Tikintisi MMC").equipmentName("Buldozer Komatsu D65EX")
                    .project(p7).notes("Gəncə-Samux yolu — 15 günlük avans qaiməsi.").build());
        }

        // PRJ-0008: Azər İnşaat (mart) — avans
        Project p8 = active.stream().filter(p -> p.getRequest() != null
                && p.getRequest().getProjectName().contains("Maştağa")).findFirst().orElse(null);
        if (p8 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.INCOME).etaxesId("ETX-2026-00224")
                    .invoiceNumber("Q-2026-0008").amount(new BigDecimal("2500.00"))
                    .invoiceDate(LocalDate.of(2026, 3, 5))
                    .companyName("Azər İnşaat MMC").equipmentName("Hidravlik Ekskavator CAT 320D")
                    .project(p8).notes("Maştağa kompleksi — 10 günlük avans qaiməsi.").build());
        }

        // ── Podratçı xərc qaimələri (B1 tipli) ───────────────────────────────

        if (c2 != null && p4 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.CONTRACTOR_EXPENSE).invoiceNumber("POD-2026-038")
                    .amount(new BigDecimal("600.00")).invoiceDate(LocalDate.of(2026, 2, 2))
                    .equipmentName("JCB 3CX Pro — icarə xidməti").contractor(c2).project(p4)
                    .notes("SkyLine layihəsi üçün TechBuild ASC texnika icarə haqqı.").build());
        }

        if (c1 != null && p5 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.CONTRACTOR_EXPENSE).invoiceNumber("POD-2026-044")
                    .amount(new BigDecimal("280.00")).invoiceDate(LocalDate.of(2026, 2, 18))
                    .equipmentName("İnvestor texnika pay xidməti").contractor(c1).project(p5)
                    .notes("AzərGold layihəsi — yüklənmə sahəsi logistika dəstəyi.").build());
        }

        if (c4 != null && p6 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.CONTRACTOR_EXPENSE).invoiceNumber("POD-2026-052")
                    .amount(new BigDecimal("1200.00")).invoiceDate(LocalDate.of(2026, 2, 28))
                    .equipmentName("Texniki servis + yağlama — LTM 1070").contractor(c4).project(p6)
                    .notes("Bakı Metro layihəsi üçün NordTex Servis QSC texniki dəstəyi.").build());
        }

        if (c3 != null && p7 != null) {
            save(Invoice.builder()
                    .type(InvoiceType.CONTRACTOR_EXPENSE).invoiceNumber("POD-2026-059")
                    .amount(new BigDecimal("450.00")).invoiceDate(LocalDate.of(2026, 3, 5))
                    .equipmentName("Kompressor icarəsi — Gəncə sahəsi").contractor(c3).project(p7)
                    .notes("Kəpəz layihəsi — havanın sıxılması üçün köməkçi kompressor.").build());
        }

        // ── Şirkət xərc qaimələri (B2 tipli) ─────────────────────────────────

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber("SRV-2026-007")
                .amount(new BigDecimal("420.00")).invoiceDate(LocalDate.of(2026, 1, 4))
                .companyName("AutoTex Servis MMC")
                .serviceDescription("EQ-002 Buldozer — mühərrik yağı dəyişimi, filtr dəsti")
                .notes("Yanvar əvvəlindən əvvəl profilaktik baxış.").build());

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber("SRV-2026-011")
                .amount(new BigDecimal("315.00")).invoiceDate(LocalDate.of(2026, 1, 19))
                .companyName("AutoTex Servis MMC")
                .serviceDescription("EQ-001 Ekskavator — texniki baxış, hidravlik şlanq")
                .project(p1)
                .notes("Binəqədi layihəsi sonunda texniki baxış.").build());

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber("SRV-2026-018")
                .amount(new BigDecimal("890.00")).invoiceDate(LocalDate.of(2026, 2, 5))
                .companyName("EQ Repair MMC")
                .serviceDescription("EQ-005 Kompaktor — mühərrik bloku, diaqnostika")
                .notes("Kompaktor mühərrik problemi. Təmir davam edir.").build());

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber("SRV-2026-023")
                .amount(new BigDecimal("180.00")).invoiceDate(LocalDate.of(2026, 2, 20))
                .companyName("Loqistik ASC")
                .serviceDescription("EQ-003 Kran — Bakıdan Metro sahəsinə daşınma")
                .project(p6)
                .notes("Kran mobilizasiya daşınması — Bakı Metro layihəsi.").build());

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber("SRV-2026-031")
                .amount(new BigDecimal("260.00")).invoiceDate(LocalDate.of(2026, 3, 1))
                .companyName("Yanacaq Bazar ASC")
                .serviceDescription("Dizel yanacaq — 280L (mart ayı qalığı doldurması)")
                .notes("Anbar yanacaq ehtiyatı yenilənməsi.").build());

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber("SRV-2026-034")
                .amount(new BigDecimal("540.00")).invoiceDate(LocalDate.of(2026, 3, 8))
                .companyName("EQ Repair MMC")
                .serviceDescription("EQ-004 Beton Mikser — növbəti texniki baxış, fırlanma sistemi")
                .notes("Planlı profilaktik baxış. İl ərzində ikinci baxış.").build());

        save(Invoice.builder()
                .type(InvoiceType.COMPANY_EXPENSE).invoiceNumber(null)
                .amount(new BigDecimal("95.00")).invoiceDate(LocalDate.of(2026, 3, 15))
                .companyName("AutoTex Servis MMC")
                .serviceDescription("EQ-006 Yük Maşını — yağ + əyləc sistemi yoxlaması")
                .notes("Qaimə nömrəsi gözlənilir. Şifahi sifariş edilib.").build());

        log.info("Mühasibatlıq seederi tamamlandı. Cəmi {} qaimə.", invoiceRepository.count());
    }

    private void save(Invoice invoice) {
        invoiceRepository.save(invoice);
    }
}
