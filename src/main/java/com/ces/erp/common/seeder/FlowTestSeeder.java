package com.ces.erp.common.seeder;

import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.operator.entity.Operator;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.projectmanager.entity.PartyType;
import com.ces.erp.projectmanager.entity.RequestShortlist;
import com.ces.erp.projectmanager.entity.ShortlistItem;
import com.ces.erp.projectmanager.repository.RequestShortlistRepository;
import com.ces.erp.projectmanager.repository.ShortlistItemRepository;
import com.ces.erp.request.entity.RequestStatusLog;
import com.ces.erp.request.entity.TechParam;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.RequestStatusLogRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test data — yeni flow-un hər mərhələsi üçün bir sorğu yaradır.
 * Yalnız DB-də sorğu yoxdursa işləyir (bir dəfə tətbiq edilir).
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class FlowTestSeeder implements CommandLineRunner {

    private final TechRequestRepository requestRepository;
    private final RequestStatusLogRepository statusLogRepository;
    private final CustomerRepository customerRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final CoordinatorPlanRepository coordinatorPlanRepository;
    private final ProjectRepository projectRepository;
    private final RequestShortlistRepository shortlistRepository;
    private final ShortlistItemRepository shortlistItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (requestRepository.count() > 0) {
            log.info("Sorğu artıq mövcuddur — FlowTestSeeder atlanır.");
            return;
        }
        log.info("Flow test sorğuları seed edilir...");

        // Reference entities
        Customer customer = customerRepository.findAll().stream().findFirst().orElse(null);
        Contractor contractor = contractorRepository.findAll().stream().findFirst().orElse(null);
        Investor investor = investorRepository.findAll().stream().findFirst().orElse(null);
        Equipment equipment = equipmentRepository.findAll().stream().findFirst().orElse(null);
        Operator operator = operatorRepository.findAll().stream().findFirst().orElse(null);

        if (customer == null || contractor == null || investor == null || equipment == null) {
            log.warn("Reference data (müştəri/podratçı/investor/texnika) yoxdur — FlowTestSeeder atlanır.");
            return;
        }

        // Hər status üçün bir sorğu yarat
        RequestStatus[] flowStatuses = {
                RequestStatus.DRAFT,
                RequestStatus.PENDING,
                RequestStatus.PM_REVIEW,
                RequestStatus.PM_SHORTLIST_READY,
                RequestStatus.COORDINATOR_NEGOTIATING,
                RequestStatus.COORDINATOR_PROPOSED,
                RequestStatus.PM_PRICE_NEGOTIATION,
                RequestStatus.PM_APPROVED,
                RequestStatus.ACCOUNTING_DOCS_CHECK,
                RequestStatus.EXECUTION_READY,
                RequestStatus.OPERATOR_ASSIGNED,
                RequestStatus.EQUIPMENT_DISPATCHED,
                RequestStatus.DELIVERED,
                RequestStatus.REJECTED,
        };

        int projectCounter = 1;
        for (int i = 0; i < flowStatuses.length; i++) {
            RequestStatus status = flowStatuses[i];
            int idx = i + 1;

            // ─── TechRequest ────────────────────────────────────────────────
            TechRequest req = TechRequest.builder()
                    .status(status)
                    .customer(customer)
                    .companyName(customer.getCompanyName())
                    .contactPerson("Test Şəxs " + idx)
                    .contactPhone("+9945012345" + String.format("%02d", idx))
                    .projectName("Test layihə #" + idx + " — " + status.name())
                    .region(idx % 2 == 0 ? "Bakı" : "Sumqayıt")
                    .requestDate(LocalDate.now().minusDays(15 - i))
                    .projectType(idx % 3 == 0 ? ProjectType.MONTHLY : ProjectType.DAILY)
                    .dayCount(idx % 3 == 0 ? 1 : 5 + idx)
                    .transportationRequired(idx % 2 == 0)
                    .notes("Test data — status: " + status.name())
                    .params(new ArrayList<>(List.of(
                            new TechParam("Yük tutumu", (10 + idx) + " ton"),
                            new TechParam("İş saatı", "8 saat")
                    )))
                    .build();
            // Razılaşdırılmış qiymət — PM_PRICE_NEGOTIATION və sonrası
            if (statusAtLeast(status, RequestStatus.PM_PRICE_NEGOTIATION)) {
                req.setAgreedTotalPrice(BigDecimal.valueOf(5000 + idx * 500));
            }
            TechRequest saved = requestRepository.save(req);
            saved.setRequestCode("REQ-" + String.format("%04d", saved.getId()));
            saved = requestRepository.save(saved);

            // Status log
            statusLogRepository.save(RequestStatusLog.builder()
                    .requestId(saved.getId())
                    .oldStatus(RequestStatus.DRAFT)
                    .newStatus(status)
                    .reason("Seed: " + status.name())
                    .changedBy("system")
                    .build());

            // ─── Shortlist (PM_REVIEW və sonrası, REJECTED-də də) ──────────
            if (status != RequestStatus.DRAFT && status != RequestStatus.PENDING) {
                RequestShortlist sl = shortlistRepository.save(
                        RequestShortlist.builder()
                                .request(saved)
                                .notes("Test shortlist — " + status.name())
                                .build());

                shortlistItemRepository.save(ShortlistItem.builder()
                        .shortlist(sl)
                        .partyType(PartyType.CONTRACTOR)
                        .contractor(contractor)
                        .equipment(equipment)
                        .negotiatedPrice(statusAtLeast(status, RequestStatus.COORDINATOR_PROPOSED)
                                ? BigDecimal.valueOf(3000) : null)
                        .notes("Test podratçı sətri")
                        .build());

                shortlistItemRepository.save(ShortlistItem.builder()
                        .shortlist(sl)
                        .partyType(PartyType.INVESTOR)
                        .investor(investor)
                        .equipment(equipment)
                        .negotiatedPrice(statusAtLeast(status, RequestStatus.COORDINATOR_PROPOSED)
                                ? BigDecimal.valueOf(3500) : null)
                        .notes("Test investor sətri")
                        .build());
            }

            // ─── CoordinatorPlan (COORDINATOR_NEGOTIATING və sonrası) ─────
            if (statusAtLeast(status, RequestStatus.COORDINATOR_NEGOTIATING)) {
                CoordinatorPlan plan = CoordinatorPlan.builder()
                        .request(saved)
                        .selectedEquipment(equipment)
                        .dayCount(saved.getDayCount())
                        .equipmentPrice(BigDecimal.valueOf(400))
                        .contractorDailyRate(BigDecimal.valueOf(300))
                        .contractorPayment(BigDecimal.valueOf(300).multiply(
                                BigDecimal.valueOf(saved.getDayCount() != null ? saved.getDayCount() : 1)))
                        .operatorPayment(BigDecimal.valueOf(150))
                        .transportationPrice(saved.isTransportationRequired() ? BigDecimal.valueOf(200) : BigDecimal.ZERO)
                        .transportContractor(saved.isTransportationRequired() ? contractor : null)
                        .startDate(LocalDate.now().plusDays(7))
                        .endDate(LocalDate.now().plusDays(7 + (saved.getDayCount() != null ? saved.getDayCount() : 1)))
                        .notes("Test koordinator planı")
                        .build();

                // Mərhələ B (icra) status sahələri
                if (statusAtLeast(status, RequestStatus.OPERATOR_ASSIGNED) && operator != null) {
                    plan.setOperator(operator);
                }
                if (statusAtLeast(status, RequestStatus.OPERATOR_ASSIGNED)) {
                    plan.setEquipmentDocsVerified(true);
                    plan.setEquipmentDocsCheckedAt(LocalDateTime.now().minusHours(2));
                }
                if (statusAtLeast(status, RequestStatus.EQUIPMENT_DISPATCHED)) {
                    plan.setDispatchedAt(LocalDateTime.now().minusHours(1));
                }
                if (status == RequestStatus.DELIVERED) {
                    plan.setDeliveredAt(LocalDateTime.now());
                    plan.setDeliveryNotes("Test təhvil-təslim qeydi");
                }
                coordinatorPlanRepository.save(plan);
            }

            // ─── Project (PM_APPROVED və sonrası) ─────────────────────────
            if (statusAtLeast(status, RequestStatus.PM_APPROVED) && status != RequestStatus.REJECTED) {
                Project project = Project.builder()
                        .projectCode("PRJ-" + String.format("%04d", projectCounter++))
                        .request(saved)
                        .status(status == RequestStatus.DELIVERED ? ProjectStatus.ACTIVE : ProjectStatus.PENDING)
                        .startDate(status == RequestStatus.DELIVERED ? LocalDate.now() : null)
                        .build();
                projectRepository.save(project);
            }
        }

        log.info("Flow test seed tamamlandı: {} sorğu yaradıldı.", flowStatuses.length);
    }

    /**
     * Status enum ordinal müqayisəsi — status1 >= status2 olduqda true qaytarır.
     * Lakin REJECTED hər zaman ayrıca işlənir (terminal status).
     */
    private boolean statusAtLeast(RequestStatus current, RequestStatus threshold) {
        if (current == RequestStatus.REJECTED) return false;
        return current.ordinal() >= threshold.ordinal();
    }
}
