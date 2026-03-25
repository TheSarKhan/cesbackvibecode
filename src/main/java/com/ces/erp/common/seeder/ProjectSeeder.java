package com.ces.erp.common.seeder;

import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.entity.ProjectExpense;
import com.ces.erp.project.entity.ProjectRevenue;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
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
@Order(9)
@RequiredArgsConstructor
@Slf4j
public class ProjectSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final TechRequestRepository requestRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (projectRepository.count() > 0) return;
        log.info("Layihələr seed edilir...");

        List<TechRequest> accepted = requestRepository
                .findAllByStatusInAndDeletedFalse(List.of(RequestStatus.ACCEPTED));

        int counter = 1;
        for (TechRequest r : accepted) {
            String co   = r.getCompanyName();
            String proj = r.getProjectName();
            String code = "PRJ-" + String.format("%04d", counter++);
            Project p   = buildProject(code, r, co, proj);
            if (p != null) {
                projectRepository.save(p);
                log.info("Layihə yaradıldı: {} — {} ({})", code, co, p.getStatus());
            }
        }

        log.info("Layihə seederi tamamlandı. Cəmi {} layihə.", projectRepository.count());
    }

    private Project buildProject(String code, TechRequest r, String co, String proj) {

        // ═══════════════════════════════════════════════════════════════════
        //  COMPLETED layihələr — yanvar-fevral 2026
        // ═══════════════════════════════════════════════════════════════════

        // PRJ-0001: Azər İnşaat — Binəqədi torpaq qazıma (yanvar 5-20)
        if (co.contains("Azər İnşaat") && proj.contains("Binəqədi")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2026, 1, 5))
                    .endDate(LocalDate.of(2026, 1, 20))
                    .hasContract(true)
                    .contractFileName("müqavilə_azər_inşaat_2026_01.pdf")
                    .scheduledHours(new BigDecimal("120.0"))
                    .actualHours(new BigDecimal("118.5"))
                    .evacuationCost(BigDecimal.ZERO)
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 85L)",         new BigDecimal("255.00"), LocalDate.of(2026, 1, 5)),
                    exp("Operator yemək/sərgi xərci",  new BigDecimal("180.00"), LocalDate.of(2026, 1, 10)),
                    exp("Yağ dəyişimi",                new BigDecimal("140.00"), LocalDate.of(2026, 1, 18)),
                    exp("Texniki baxış",               new BigDecimal("220.00"), LocalDate.of(2026, 1, 20))
            ));
            attachRevenues(p, List.of(
                    rev("Texnika icarəsi (15 gün × 200 AZN)",  new BigDecimal("3000.00"), LocalDate.of(2026, 1, 5)),
                    rev("Operator haqqı",                       new BigDecimal("750.00"),  LocalDate.of(2026, 1, 5))
            ));
            return p;
        }

        // PRJ-0002: Grand Build — Sabunçu bina kran (yanvar 10-22)
        if (co.contains("Grand Build") && proj.contains("Sabunçu")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2026, 1, 10))
                    .endDate(LocalDate.of(2026, 1, 22))
                    .hasContract(true)
                    .contractFileName("müqavilə_grand_build_2026_01.pdf")
                    .scheduledHours(new BigDecimal("96.0"))
                    .actualHours(new BigDecimal("94.0"))
                    .evacuationCost(BigDecimal.ZERO)
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 130L)",        new BigDecimal("390.00"), LocalDate.of(2026, 1, 10)),
                    exp("Operator yemək/sərgi xərci",  new BigDecimal("240.00"), LocalDate.of(2026, 1, 16)),
                    exp("Daşınma xərci (icarəçiyə)",   new BigDecimal("600.00"), LocalDate.of(2026, 1, 10)),
                    exp("Texniki yağlama",              new BigDecimal("95.00"),  LocalDate.of(2026, 1, 22))
            ));
            attachRevenues(p, List.of(
                    rev("Kran icarəsi (12 gün × 400 AZN)", new BigDecimal("4800.00"), LocalDate.of(2026, 1, 10)),
                    rev("Operator haqqı",                   new BigDecimal("720.00"),  LocalDate.of(2026, 1, 10)),
                    rev("Daşınma xidməti",                  new BigDecimal("600.00"),  LocalDate.of(2026, 1, 10))
            ));
            return p;
        }

        // PRJ-0003: SOCAR — Balaxanı buldozer (yanvar 16 - fevral 3)
        if (co.contains("SOCAR") && proj.contains("Balaxanı")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2026, 1, 16))
                    .endDate(LocalDate.of(2026, 2, 3))
                    .hasContract(true)
                    .contractFileName("müqavilə_socar_tikinti_2026_01.pdf")
                    .scheduledHours(new BigDecimal("144.0"))
                    .actualHours(new BigDecimal("146.5"))
                    .evacuationCost(new BigDecimal("350.00"))
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 200L)",          new BigDecimal("600.00"), LocalDate.of(2026, 1, 16)),
                    exp("Yanacaq (əlavə, 80L)",           new BigDecimal("240.00"), LocalDate.of(2026, 1, 25)),
                    exp("Operator yemək/sərgi",           new BigDecimal("360.00"), LocalDate.of(2026, 1, 20)),
                    exp("Daşınma xərci (getmə)",          new BigDecimal("400.00"), LocalDate.of(2026, 1, 15)),
                    exp("Evakuator (texnika geri dönüş)", new BigDecimal("350.00"), LocalDate.of(2026, 2, 3)),
                    exp("Texniki baxış (ATEX standartı)", new BigDecimal("280.00"), LocalDate.of(2026, 2, 4))
            ));
            attachRevenues(p, List.of(
                    rev("Buldozer icarəsi (18 gün × 180 AZN)", new BigDecimal("3240.00"), LocalDate.of(2026, 1, 16)),
                    rev("Operator haqqı",                        new BigDecimal("900.00"),  LocalDate.of(2026, 1, 16)),
                    rev("Daşınma xidməti",                       new BigDecimal("800.00"),  LocalDate.of(2026, 1, 16))
            ));
            return p;
        }

        // PRJ-0004: SkyLine — Sumqayıt çimərlik (yanvar 22 - fevral 1)
        if (co.contains("SkyLine") && proj.contains("çimərlik")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2026, 1, 22))
                    .endDate(LocalDate.of(2026, 2, 1))
                    .hasContract(true)
                    .contractFileName("müqavilə_skyline_2026_01.pdf")
                    .scheduledHours(new BigDecimal("80.0"))
                    .actualHours(new BigDecimal("79.0"))
                    .evacuationCost(BigDecimal.ZERO)
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 60L)",        new BigDecimal("180.00"), LocalDate.of(2026, 1, 22)),
                    exp("Operator yemək/sərgi xərci", new BigDecimal("200.00"), LocalDate.of(2026, 1, 26)),
                    exp("Podratçı xidmət haqqı",      new BigDecimal("600.00"), LocalDate.of(2026, 2, 1))
            ));
            attachRevenues(p, List.of(
                    rev("Ekskavator icarəsi (10 gün × 160 AZN)", new BigDecimal("1600.00"), LocalDate.of(2026, 1, 22)),
                    rev("Operator haqqı",                          new BigDecimal("400.00"),  LocalDate.of(2026, 1, 22))
            ));
            return p;
        }

        // PRJ-0005: AzərGold — Daşkəsən teleskopik (fevral 4-18)
        if (co.contains("AzərGold") && proj.contains("yüklənmə")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2026, 2, 4))
                    .endDate(LocalDate.of(2026, 2, 18))
                    .hasContract(true)
                    .contractFileName("müqavilə_azergold_2026_02.pdf")
                    .scheduledHours(new BigDecimal("112.0"))
                    .actualHours(new BigDecimal("110.0"))
                    .evacuationCost(BigDecimal.ZERO)
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 90L)",        new BigDecimal("270.00"), LocalDate.of(2026, 2, 4)),
                    exp("Operator yemək/sərgi",        new BigDecimal("280.00"), LocalDate.of(2026, 2, 10)),
                    exp("Daşınma (Gəncəyə)",          new BigDecimal("450.00"), LocalDate.of(2026, 2, 3)),
                    exp("Daşınma (Bakıya dönüş)",     new BigDecimal("450.00"), LocalDate.of(2026, 2, 19)),
                    exp("İnvestor texnika pay xərci", new BigDecimal("280.00"), LocalDate.of(2026, 2, 18))
            ));
            attachRevenues(p, List.of(
                    rev("Yükləyici icarəsi (14 gün × 200 AZN)", new BigDecimal("2800.00"), LocalDate.of(2026, 2, 4)),
                    rev("Operator haqqı",                         new BigDecimal("700.00"),  LocalDate.of(2026, 2, 4)),
                    rev("Daşınma xidməti",                        new BigDecimal("900.00"),  LocalDate.of(2026, 2, 4))
            ));
            return p;
        }

        // ═══════════════════════════════════════════════════════════════════
        //  ACTIVE layihələr — fevral-mart 2026
        // ═══════════════════════════════════════════════════════════════════

        // PRJ-0006: Bakı Metro — Kran aylıq (fevral 15 - mart 15)
        if (co.contains("Bakı Metro") && proj.contains("8 Noyabr")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.of(2026, 2, 15))
                    .endDate(LocalDate.of(2026, 3, 15))
                    .hasContract(true)
                    .contractFileName("müqavilə_metro_2026_02.pdf")
                    .scheduledHours(new BigDecimal("224.0"))
                    .actualHours(new BigDecimal("190.0"))
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 280L)",        new BigDecimal("840.00"), LocalDate.of(2026, 2, 15)),
                    exp("Yanacaq (dizel, 250L)",        new BigDecimal("750.00"), LocalDate.of(2026, 3, 1)),
                    exp("Operator yemək/sərgi (fevral)", new BigDecimal("560.00"), LocalDate.of(2026, 2, 28)),
                    exp("Daşınma (mobilizasiya)",        new BigDecimal("600.00"), LocalDate.of(2026, 2, 14)),
                    exp("Texniki yağlama",               new BigDecimal("220.00"), LocalDate.of(2026, 3, 5))
            ));
            attachRevenues(p, List.of(
                    rev("Kran icarəsi — fevral avansı (14 gün × 400 AZN)", new BigDecimal("5600.00"), LocalDate.of(2026, 2, 15)),
                    rev("Operator haqqı — fevral",                           new BigDecimal("1400.00"), LocalDate.of(2026, 2, 15)),
                    rev("Daşınma xidməti",                                   new BigDecimal("1200.00"), LocalDate.of(2026, 2, 15))
            ));
            return p;
        }

        // PRJ-0007: Kəpəz — Gəncə-Samux buldozer (fevral 24 - mart 26)
        if (co.contains("Kəpəz") && proj.contains("magistral")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.of(2026, 2, 24))
                    .endDate(LocalDate.of(2026, 3, 26))
                    .hasContract(true)
                    .contractFileName("müqavilə_kepez_2026_02.pdf")
                    .scheduledHours(new BigDecimal("240.0"))
                    .actualHours(new BigDecimal("192.0"))
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 250L)",        new BigDecimal("750.00"), LocalDate.of(2026, 2, 24)),
                    exp("Yanacaq (dizel, 220L)",        new BigDecimal("660.00"), LocalDate.of(2026, 3, 10)),
                    exp("Operator yemək/sərgi (fevral)", new BigDecimal("120.00"), LocalDate.of(2026, 2, 28)),
                    exp("Operator yemək/sərgi (mart)",   new BigDecimal("390.00"), LocalDate.of(2026, 3, 15)),
                    exp("Daşınma (Gəncəyə)",            new BigDecimal("250.00"), LocalDate.of(2026, 2, 23))
            ));
            attachRevenues(p, List.of(
                    rev("Buldozer icarəsi avansı (15 gün × 180 AZN)", new BigDecimal("2700.00"), LocalDate.of(2026, 2, 24)),
                    rev("Operator haqqı — avans",                       new BigDecimal("750.00"),  LocalDate.of(2026, 2, 24)),
                    rev("Daşınma xidməti",                              new BigDecimal("500.00"),  LocalDate.of(2026, 2, 24))
            ));
            return p;
        }

        // PRJ-0008: Azər İnşaat — Maştağa ekskavator (mart 5-25)
        if (co.contains("Azər İnşaat") && proj.contains("Maştağa")) {
            Project p = Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.of(2026, 3, 5))
                    .endDate(LocalDate.of(2026, 3, 25))
                    .hasContract(true)
                    .contractFileName("müqavilə_azər_inşaat_2026_03.pdf")
                    .scheduledHours(new BigDecimal("160.0"))
                    .actualHours(new BigDecimal("120.0"))
                    .build();
            attachExpenses(p, List.of(
                    exp("Yanacaq (dizel, 140L)",         new BigDecimal("420.00"), LocalDate.of(2026, 3, 5)),
                    exp("Operator yemək/sərgi",          new BigDecimal("300.00"), LocalDate.of(2026, 3, 15)),
                    exp("Texniki yağlama (müntəzəm)",    new BigDecimal("120.00"), LocalDate.of(2026, 3, 18))
            ));
            attachRevenues(p, List.of(
                    rev("Ekskavator icarəsi avansı (10 gün × 200 AZN)", new BigDecimal("2000.00"), LocalDate.of(2026, 3, 5)),
                    rev("Operator haqqı — avans",                         new BigDecimal("500.00"),  LocalDate.of(2026, 3, 5))
            ));
            return p;
        }

        // ═══════════════════════════════════════════════════════════════════
        //  PENDING layihə — mart 2026, müqavilə gözlənilir
        // ═══════════════════════════════════════════════════════════════════

        // PRJ-0009: Grand Build — Xəzər villa ekskavator (başlamayıb)
        if (co.contains("Grand Build") && proj.contains("villa")) {
            return Project.builder()
                    .projectCode(code).request(r)
                    .status(ProjectStatus.PENDING)
                    .startDate(LocalDate.of(2026, 3, 15))
                    .endDate(LocalDate.of(2026, 4, 9))
                    .hasContract(false)
                    .build();
        }

        return null;
    }

    // ─── Köməkçi metodlar ────────────────────────────────────────────────────

    private void attachExpenses(Project p, List<ProjectExpense> expenses) {
        for (ProjectExpense e : expenses) {
            e.setProject(p);
            p.getExpenses().add(e);
        }
    }

    private void attachRevenues(Project p, List<ProjectRevenue> revenues) {
        for (ProjectRevenue rv : revenues) {
            rv.setProject(p);
            p.getRevenues().add(rv);
        }
    }

    private ProjectExpense exp(String key, BigDecimal val, LocalDate date) {
        return ProjectExpense.builder().key(key).value(val).date(date).build();
    }

    private ProjectRevenue rev(String key, BigDecimal val, LocalDate date) {
        return ProjectRevenue.builder().key(key).value(val).date(date).build();
    }
}
