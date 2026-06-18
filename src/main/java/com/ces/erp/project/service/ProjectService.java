package com.ces.erp.project.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.entity.CoordinatorPlanItem;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.entity.EquipmentProjectHistory;
import com.ces.erp.garage.repository.EquipmentProjectHistoryRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.project.dto.FinanceEntryRequest;
import com.ces.erp.project.dto.ProjectCompleteRequest;
import com.ces.erp.project.dto.ProjectPaymentEntryRequest;
import com.ces.erp.project.dto.ProjectPaymentEntryResponse;
import com.ces.erp.project.dto.ProjectResponse;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.entity.ProjectExpense;
import com.ces.erp.project.entity.ProjectPaymentEntry;
import com.ces.erp.project.entity.ProjectRevenue;
import com.ces.erp.project.repository.ProjectExpenseRepository;
import com.ces.erp.project.repository.ProjectPaymentEntryRepository;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.project.repository.ProjectRevenueRepository;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.OwnershipType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectExpenseRepository expenseRepository;
    private final ProjectRevenueRepository revenueRepository;
    private final ProjectPaymentEntryRepository paymentEntryRepository;
    private final CoordinatorPlanRepository planRepository;
    private final EquipmentProjectHistoryRepository equipmentHistoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final com.ces.erp.garage.service.EquipmentService equipmentService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final InvoiceRepository invoiceRepository;

    // ─── List ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll() {
        return projectRepository.findAllWithFinances().stream()
                .map(p -> {
                    CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
                    return ProjectResponse.from(p, plan);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> getAllPaged(int page, int size, String search, String status) {
        String q = (search != null && !search.isBlank()) ? search : null;
        ProjectStatus s = (status != null && !status.isBlank()) ? ProjectStatus.valueOf(status) : null;
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = projectRepository.findAllFiltered(q, s, pageable);
        return PagedResponse.from(result, p -> {
            CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
            return ProjectResponse.from(p, plan);
        });
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project p = findOrThrow(id);
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    // ─── Müqavilə endirmə ─────────────────────────────────────────────────────
    // QEYD: Müqavilə YÜKLƏMƏ silindi — layihə artıq müqavilə ilə deyil, mühasibat OK +
    // Əməliyyatların təsdiqi ilə ACTIVE olur (bax DocumentCheckService.submitForActivation).
    // Mövcud layihələrin müqaviləsini endirmək üçün resolveContract saxlanılır.

    @Transactional(readOnly = true)
    public Path resolveContract(Long id) {
        Project p = findOrThrow(id);
        if (p.getContractFilePath() == null) {
            throw new BusinessException("Bu layihənin müqavilə sənədi yoxdur");
        }
        return fileStorageService.resolve(p.getContractFilePath());
    }

    // ─── Maliyyə — Xərclər ────────────────────────────────────────────────────

    public ProjectResponse.FinancesDto getFinances(Long id) {
        findOrThrow(id);

        List<ProjectResponse.FinanceEntryDto> expenses = expenseRepository
                .findAllByProjectIdAndDeletedFalse(id).stream()
                .map(e -> ProjectResponse.FinanceEntryDto.builder()
                        .id(e.getId())
                        .key(e.getKey())
                        .value(e.getValue())
                        .date(e.getDate())
                        .build())
                .toList();

        List<ProjectResponse.FinanceEntryDto> revenues = revenueRepository
                .findAllByProjectIdAndDeletedFalse(id).stream()
                .map(r -> ProjectResponse.FinanceEntryDto.builder()
                        .id(r.getId())
                        .key(r.getKey())
                        .value(r.getValue())
                        .date(r.getDate())
                        .build())
                .toList();

        return ProjectResponse.FinancesDto.builder()
                .expenses(expenses)
                .revenues(revenues)
                .build();
    }

    @Transactional
    public ProjectResponse.FinanceEntryDto addExpense(Long id, FinanceEntryRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Xərc yalnız aktiv layihəyə əlavə edilə bilər");
        }

        ProjectExpense expense = ProjectExpense.builder()
                .project(p)
                .key(req.getKey())
                .value(req.getValue())
                .date(LocalDate.now())
                .build();

        expense = expenseRepository.save(expense);
        return ProjectResponse.FinanceEntryDto.builder()
                .id(expense.getId())
                .key(expense.getKey())
                .value(expense.getValue())
                .date(expense.getDate())
                .build();
    }

    @Transactional
    public void deleteExpense(Long id, Long expenseId) {
        findOrThrow(id);
        ProjectExpense expense = expenseRepository.findByIdAndProjectIdAndDeletedFalse(expenseId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Xərc", expenseId));
        expense.softDelete();
        expenseRepository.save(expense);
    }

    // ─── Maliyyə — Gəlirlər ───────────────────────────────────────────────────

    @Transactional
    public ProjectResponse.FinanceEntryDto addRevenue(Long id, FinanceEntryRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Gəlir yalnız aktiv layihəyə əlavə edilə bilər");
        }

        ProjectRevenue revenue = ProjectRevenue.builder()
                .project(p)
                .key(req.getKey())
                .value(req.getValue())
                .date(LocalDate.now())
                .build();

        revenue = revenueRepository.save(revenue);
        return ProjectResponse.FinanceEntryDto.builder()
                .id(revenue.getId())
                .key(revenue.getKey())
                .value(revenue.getValue())
                .date(revenue.getDate())
                .build();
    }

    @Transactional
    public void deleteRevenue(Long id, Long revenueId) {
        findOrThrow(id);
        ProjectRevenue revenue = revenueRepository.findByIdAndProjectIdAndDeletedFalse(revenueId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Gəlir", revenueId));
        revenue.softDelete();
        revenueRepository.save(revenue);
    }

    // ─── Layihəni bitir ───────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse complete(Long id, ProjectCompleteRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Yalnız ACTIVE statuslu layihə bağlana bilər");
        }

        // Layihəni bitirmədən əvvəl ən az bir təsdiqlənmiş qaimə olmalıdır
        boolean hasApprovedInvoice = invoiceRepository
                .existsByProjectIdAndStatusAndDeletedFalse(p.getId(), InvoiceStatus.APPROVED);
        if (!hasApprovedInvoice) {
            throw new BusinessException("Layihəni bitirmək üçün ən az bir təsdiqlənmiş qaimə (qəbul sənədi) olmalıdır");
        }

        // Bitmə tarixi — bağlanış zamanı qeyd olunur (effektiv gün hesablamasından əvvəl tətbiq et)
        if (req.getEndDate() != null) {
            p.setEndDate(req.getEndDate());
        }

        // Planlaşdırılan saatlar: effektiv gün × 9 (1 gün = 9 saat)
        CoordinatorPlan planForHours = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        Integer planDayCount = planForHours != null && planForHours.getDayCount() != null
                ? planForHours.getDayCount()
                : (p.getRequest() != null ? p.getRequest().getDayCount() : null);

        // Gap 1: Əgər layihənin faktiki start/end tarixləri varsa, onlardan effektiv gün sayını hesabla
        int effectiveDayCount;
        if (p.getStartDate() != null && p.getEndDate() != null) {
            long actualDays = ChronoUnit.DAYS.between(p.getStartDate(), p.getEndDate());
            effectiveDayCount = actualDays > 0 ? (int) actualDays : (planDayCount != null ? planDayCount : 0);
        } else {
            effectiveDayCount = planDayCount != null ? planDayCount : 0;
        }
        BigDecimal scheduled = effectiveDayCount > 0
                ? BigDecimal.valueOf(effectiveDayCount).multiply(BigDecimal.valueOf(9))
                : BigDecimal.ZERO;

        BigDecimal actual = req.getActualHours() != null ? req.getActualHours() : scheduled;
        BigDecimal overtimeRate = req.getOvertimeRate() != null ? req.getOvertimeRate() : BigDecimal.ONE;
        BigDecimal overtimeHours = actual.subtract(scheduled).max(BigDecimal.ZERO);

        // Çoxlu texnika: əlavə vaxt saatlıq dərəcəsi BÜTÜN xətlərin gündəlik dəyərinin cəmindən
        // DAILY → equipmentPrice artıq gündəlikdir; MONTHLY → 26 iş gününə bölünür.
        ProjectType projectType = p.getRequest() != null ? p.getRequest().getProjectType() : null;
        List<CoordinatorPlanItem> planItems = planForHours != null
                ? planForHours.getItems().stream().filter(i -> !i.isDeleted()).toList()
                : List.of();
        BigDecimal totalDailyRate = BigDecimal.ZERO;
        if (!planItems.isEmpty()) {
            for (CoordinatorPlanItem it : planItems) {
                BigDecimal ep = it.getEquipmentPrice() != null ? it.getEquipmentPrice() : BigDecimal.ZERO;
                totalDailyRate = totalDailyRate.add(projectType == ProjectType.MONTHLY
                        ? ep.divide(BigDecimal.valueOf(26), 4, RoundingMode.HALF_UP) : ep);
            }
        } else {
            BigDecimal ep = planForHours != null && planForHours.getEquipmentPrice() != null
                    ? planForHours.getEquipmentPrice() : BigDecimal.ZERO;
            totalDailyRate = projectType == ProjectType.MONTHLY
                    ? ep.divide(BigDecimal.valueOf(26), 4, RoundingMode.HALF_UP) : ep;
        }
        BigDecimal hourlyRate = totalDailyRate.divide(BigDecimal.valueOf(9), 4, RoundingMode.HALF_UP);
        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(overtimeRate).setScale(2, RoundingMode.HALF_UP);

        p.setEvacuationCost(req.getEvacuationCost());
        p.setScheduledHours(scheduled);
        p.setActualHours(actual);
        p.setOvertimeHours(overtimeHours);
        p.setOvertimeRate(overtimeRate);
        p.setOvertimePay(overtimePay);

        // Gap 4: Əlavə vaxt haqqını gəlir kimi qeyd et
        if (overtimePay.compareTo(BigDecimal.ZERO) > 0) {
            String rateLabel = overtimeRate.compareTo(BigDecimal.ONE) == 0 ? "1×" : "1.5×";
            ProjectRevenue overtimeRevenue = ProjectRevenue.builder()
                    .project(p)
                    .key("Əlavə vaxt haqqı (" + rateLabel + ")")
                    .value(overtimePay)
                    .date(LocalDate.now())
                    .build();
            revenueRepository.save(overtimeRevenue);
        }

        p.setStatus(ProjectStatus.COMPLETED);
        if (p.getEndDate() == null) {
            p.setEndDate(LocalDate.now());
        }

        projectRepository.save(p);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "YENİLƏNDİ", "Layihə tamamlandı");
        CoordinatorPlan plan = planForHours;

        // Texnikaların layihə tarixçəsi — hər texnika xətti üçün (çoxlu model).
        // Sahib (podratçı/investor) xərc qaimələri qaimə axınında (autoCreateExpenseInvoice)
        // per-line yaranır — burada dublikat yaratmırıq.
        String histProjectName = p.getRequest() != null ? p.getRequest().getProjectName() : p.getProjectCode();
        String histNotes = p.getRequest() != null ? p.getRequest().getCompanyName() : null;
        if (!planItems.isEmpty()) {
            for (CoordinatorPlanItem it : planItems) {
                Equipment le = it.getEquipment();
                if (le == null) continue;
                EquipmentProjectHistory history = EquipmentProjectHistory.builder()
                        .equipment(le)
                        .projectId(p.getId())
                        .projectName(histProjectName)
                        .startDate(it.getStartDate() != null ? it.getStartDate() : p.getStartDate())
                        .endDate(it.getEndDate() != null ? it.getEndDate() : p.getEndDate())
                        .contractorRevenue(lineCostTotal(it, projectType))
                        .status("COMPLETED")
                        .notes(histNotes)
                        .build();
                equipmentHistoryRepository.save(history);

                if (le.getStatus() == EquipmentStatus.RENTED) {
                    equipmentService.changeStatus(le, EquipmentStatus.IN_TRANSIT,
                            "Layihə tamamlandı — texnika geri yoldadır", equipmentService.currentUserOrNull());
                }
            }
        } else {
            // Legacy tək-texnika (item-siz köhnə planlar)
            Equipment eq = plan != null && plan.getSelectedEquipment() != null
                    ? plan.getSelectedEquipment()
                    : (p.getRequest() != null ? p.getRequest().getSelectedEquipment() : null);
            if (eq != null) {
                EquipmentProjectHistory history = EquipmentProjectHistory.builder()
                        .equipment(eq)
                        .projectId(p.getId())
                        .projectName(histProjectName)
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .contractorRevenue(plan != null && plan.getContractorPayment() != null
                                ? plan.getContractorPayment() : BigDecimal.ZERO)
                        .status("COMPLETED")
                        .notes(histNotes)
                        .build();
                equipmentHistoryRepository.save(history);

                if (eq.getStatus() == EquipmentStatus.RENTED) {
                    equipmentService.changeStatus(eq, EquipmentStatus.IN_TRANSIT,
                            "Layihə tamamlandı — texnika geri yoldadır", equipmentService.currentUserOrNull());
                }
            }
        }

        return ProjectResponse.from(p, plan);
    }

    /** Bir texnika xəttinin maya dəyəri cəmi (DAILY: vahid×gün, MONTHLY: vahid). */
    private static BigDecimal lineCostTotal(CoordinatorPlanItem it, ProjectType type) {
        BigDecimal unit = it.getEquipmentPrice() != null ? it.getEquipmentPrice() : BigDecimal.ZERO;
        int days = it.getDayCount() != null ? it.getDayCount() : 0;
        if (type == ProjectType.MONTHLY || days == 0) return unit;
        return unit.multiply(BigDecimal.valueOf(days));
    }

    // ─── Bitmə tarixini yenilə ────────────────────────────────────────────────

    @Transactional
    public ProjectResponse updateStartDate(Long id, LocalDate startDate) {
        Project p = findOrThrow(id);
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("Bağlanmış layihənin başlanğıc tarixi dəyişdirilə bilməz");
        }
        p.setStartDate(startDate);
        projectRepository.save(p);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "YENİLƏNDİ", "Başlanğıc tarixi yeniləndi");
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    @Transactional
    public ProjectResponse updateEndDate(Long id, LocalDate endDate) {
        Project p = findOrThrow(id);
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("Bağlanmış layihənin bitmə tarixi dəyişdirilə bilməz");
        }
        p.setEndDate(endDate);
        projectRepository.save(p);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "YENİLƏNDİ", "Layihə yeniləndi");
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    // ─── Ödəniş girişləri ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectPaymentEntryResponse> getPaymentEntries(Long id) {
        findOrThrow(id);
        return paymentEntryRepository
                .findAllByProjectIdAndDeletedFalseOrderByPaymentDateAsc(id)
                .stream()
                .map(ProjectPaymentEntryResponse::from)
                .toList();
    }

    @Transactional
    public ProjectPaymentEntryResponse addPaymentEntry(Long id, ProjectPaymentEntryRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Ödəniş yalnız aktiv layihəyə əlavə edilə bilər");
        }
        ProjectPaymentEntry entry = ProjectPaymentEntry.builder()
                .project(p)
                .amount(req.getAmount())
                .paymentDate(req.getPaymentDate())
                .note(req.getNote())
                .build();
        entry = paymentEntryRepository.save(entry);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "ÖDƏNIŞ",
                "Ödəniş girişi əlavə edildi: " + req.getAmount() + " ₼");
        return ProjectPaymentEntryResponse.from(entry);
    }

    @Transactional
    public void deletePaymentEntry(Long id, Long entryId) {
        findOrThrow(id);
        ProjectPaymentEntry entry = paymentEntryRepository
                .findByIdAndProjectIdAndDeletedFalse(entryId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödəniş girişi", entryId));
        entry.softDelete();
        paymentEntryRepository.save(entry);
    }

    @Transactional
    public void closePayment(Long id) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Ödəniş yalnız aktiv layihədə bağlana bilər");
        }
        List<ProjectPaymentEntry> entries = paymentEntryRepository
                .findAllByProjectIdAndDeletedFalseOrderByPaymentDateAsc(id);
        if (entries.isEmpty()) {
            throw new BusinessException("Bağlamaq üçün ən az bir ödəniş girişi olmalıdır");
        }
        // Bütün girişləri bağlandı kimi qeyd et
        entries.forEach(e -> e.setClosed(true));
        paymentEntryRepository.saveAll(entries);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "ÖDƏNIŞ BAĞLANDI",
                "Ödəniş seriyası bağlandı");
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findByIdWithFinances(id)
                .orElseThrow(() -> new ResourceNotFoundException("Layihə", id));
    }
}
