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
@Order(7)
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

        List<TechRequest> acceptedRequests = requestRepository
                .findAllByStatusInAndDeletedFalse(List.of(RequestStatus.ACCEPTED));

        if (acceptedRequests.isEmpty()) {
            log.warn("ACCEPTED statuslu sorğu tapılmadı, layihə seed edilmir.");
            return;
        }

        // 1. PENDING — müqavilə gözlənilir
        // İlk ACCEPTED sorğudan PENDING layihə yarat
        TechRequest req1 = acceptedRequests.get(0);
        Project pending = Project.builder()
                .projectCode("PRJ-" + String.format("%04d", 1))
                .request(req1)
                .status(ProjectStatus.PENDING)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .hasContract(false)
                .build();
        projectRepository.save(pending);
        log.info("PENDING layihə yaradıldı: {} → {}", pending.getProjectCode(), req1.getCompanyName());

        // 2. ACTIVE — müqavilə var, maliyyə əməliyyatları gedir
        // Eyni sorğudan ya da hardcoded müstəqil layihə (demo üçün)
        // Əgər ikinci ACCEPTED sorğu varsa istifadə edirik, əks halda yeni request axtarırıq
        TechRequest req2 = acceptedRequests.size() > 1
                ? acceptedRequests.get(1)
                : req1;

        if (!projectRepository.existsByRequestIdAndDeletedFalse(req2.getId()) || req2.getId().equals(req1.getId())) {
            // Eyni request-ə iki layihə yaratmaq olmaz — ACTIVE-i manual yaradaq
            // Əgər req2 == req1 olarsa, əlavə sorğu tapmağa çalışaq
            List<TechRequest> offerSentRequests = requestRepository
                    .findAllByStatusInAndDeletedFalse(List.of(RequestStatus.OFFER_SENT));

            if (!offerSentRequests.isEmpty()) {
                TechRequest req2alt = offerSentRequests.get(0);
                if (!projectRepository.existsByRequestIdAndDeletedFalse(req2alt.getId())) {
                    Project active = buildActiveProject(req2alt, 2);
                    projectRepository.save(active);
                    log.info("ACTIVE layihə yaradıldı: {} → {}", active.getProjectCode(), req2alt.getCompanyName());
                }
            }
        } else {
            Project active = buildActiveProject(req2, 2);
            projectRepository.save(active);
            log.info("ACTIVE layihə yaradıldı: {} → {}", active.getProjectCode(), req2.getCompanyName());
        }

        // 3. COMPLETED — bağlanmış layihə
        List<TechRequest> sentRequests = requestRepository
                .findAllByStatusInAndDeletedFalse(List.of(RequestStatus.SENT_TO_COORDINATOR));

        if (!sentRequests.isEmpty()) {
            TechRequest req3 = sentRequests.get(0);
            if (!projectRepository.existsByRequestIdAndDeletedFalse(req3.getId())) {
                Project completed = buildCompletedProject(req3, 3);
                projectRepository.save(completed);
                log.info("COMPLETED layihə yaradıldı: {} → {}", completed.getProjectCode(), req3.getCompanyName());
            }
        }

        log.info("Layihə seederi tamamlandı. Cəmi {} layihə.", projectRepository.count());
    }

    // ─── ACTIVE layihə ────────────────────────────────────────────────────────

    private Project buildActiveProject(TechRequest req, int num) {
        ProjectExpense exp1 = ProjectExpense.builder()
                .key("Benzin")
                .value(new BigDecimal("320.00"))
                .date(LocalDate.of(2026, 2, 20))
                .build();

        ProjectExpense exp2 = ProjectExpense.builder()
                .key("Operator yemək xərci")
                .value(new BigDecimal("180.00"))
                .date(LocalDate.of(2026, 2, 22))
                .build();

        ProjectExpense exp3 = ProjectExpense.builder()
                .key("Yağ və texniki maye")
                .value(new BigDecimal("95.00"))
                .date(LocalDate.of(2026, 2, 24))
                .build();

        ProjectRevenue rev1 = ProjectRevenue.builder()
                .key("Texnika icarəsi")
                .value(new BigDecimal("2100.00"))
                .date(LocalDate.of(2026, 2, 20))
                .build();

        ProjectRevenue rev2 = ProjectRevenue.builder()
                .key("Operator haqqı")
                .value(new BigDecimal("450.00"))
                .date(LocalDate.of(2026, 2, 20))
                .build();

        Project project = Project.builder()
                .projectCode("PRJ-" + String.format("%04d", num))
                .request(req)
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 2, 17))
                .endDate(LocalDate.of(2026, 3, 17))
                .hasContract(true)
                .contractFileName("müqavilə_2026_02.pdf")
                .build();

        // İki tərəfli əlaqə
        exp1.setProject(project);
        exp2.setProject(project);
        exp3.setProject(project);
        rev1.setProject(project);
        rev2.setProject(project);

        project.getExpenses().addAll(List.of(exp1, exp2, exp3));
        project.getRevenues().addAll(List.of(rev1, rev2));

        return project;
    }

    // ─── COMPLETED layihə ─────────────────────────────────────────────────────

    private Project buildCompletedProject(TechRequest req, int num) {
        ProjectExpense exp1 = ProjectExpense.builder()
                .key("Benzin")
                .value(new BigDecimal("210.00"))
                .date(LocalDate.of(2026, 2, 5))
                .build();

        ProjectExpense exp2 = ProjectExpense.builder()
                .key("Operator əlavə xərci")
                .value(new BigDecimal("120.00"))
                .date(LocalDate.of(2026, 2, 8))
                .build();

        ProjectExpense exp3 = ProjectExpense.builder()
                .key("Evakuator")
                .value(new BigDecimal("350.00"))
                .date(LocalDate.of(2026, 2, 15))
                .build();

        ProjectRevenue rev1 = ProjectRevenue.builder()
                .key("Texnika icarəsi")
                .value(new BigDecimal("1400.00"))
                .date(LocalDate.of(2026, 2, 5))
                .build();

        ProjectRevenue rev2 = ProjectRevenue.builder()
                .key("Daşınma xidməti")
                .value(new BigDecimal("600.00"))
                .date(LocalDate.of(2026, 2, 5))
                .build();

        Project project = Project.builder()
                .projectCode("PRJ-" + String.format("%04d", num))
                .request(req)
                .status(ProjectStatus.COMPLETED)
                .startDate(LocalDate.of(2026, 2, 5))
                .endDate(LocalDate.of(2026, 2, 15))
                .hasContract(true)
                .contractFileName("müqavilə_fevral_2026.pdf")
                .evacuationCost(new BigDecimal("350.00"))
                .scheduledHours(new BigDecimal("80.0"))
                .actualHours(new BigDecimal("76.5"))
                .build();

        exp1.setProject(project);
        exp2.setProject(project);
        exp3.setProject(project);
        rev1.setProject(project);
        rev2.setProject(project);

        project.getExpenses().addAll(List.of(exp1, exp2, exp3));
        project.getRevenues().addAll(List.of(rev1, rev2));

        return project;
    }
}
