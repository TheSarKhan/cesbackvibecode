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
import com.ces.erp.project.repository.ProjectDowntimeRepository;
import com.ces.erp.project.repository.ProjectEquipmentSwapRepository;
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
    private final ProjectDowntimeRepository downtimeRepository;
    private final ProjectEquipmentSwapRepository swapRepository;
    private final CoordinatorPlanRepository planRepository;
    private final EquipmentProjectHistoryRepository equipmentHistoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final com.ces.erp.garage.service.EquipmentService equipmentService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final InvoiceRepository invoiceRepository;
    private final com.ces.erp.common.notification.service.WorkflowTelegramNotificationService workflowTelegramService;

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

    @Transactional
    public ProjectResponse returnToGarage(Long id, com.ces.erp.project.dto.ProjectDemobilizeRequest req) {
        Project p = findOrThrow(id);
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);

        if (p.getStatus() != ProjectStatus.COMPLETED) {
            p.setStatus(ProjectStatus.COMPLETED);
            if (p.getEndDate() == null) p.setEndDate(LocalDate.now());
            projectRepository.save(p);
        }

        List<Equipment> targetEquipments = new java.util.ArrayList<>();
        if (plan != null && plan.getItems() != null && !plan.getItems().isEmpty()) {
            for (CoordinatorPlanItem it : plan.getItems()) {
                if (it.getEquipment() != null && !it.isDeleted()) {
                    targetEquipments.add(it.getEquipment());
                }
            }
        }
        if (targetEquipments.isEmpty()) {
            Equipment eq = plan != null && plan.getSelectedEquipment() != null
                    ? plan.getSelectedEquipment()
                    : (p.getRequest() != null ? p.getRequest().getSelectedEquipment() : null);
            if (eq != null) targetEquipments.add(eq);
        }

        EquipmentStatus nextStatus = (req != null && req.isRequiresInspection())
                ? EquipmentStatus.IN_INSPECTION
                : EquipmentStatus.AVAILABLE;

        for (Equipment eq : targetEquipments) {
            if (req != null && req.getFinalHourKmCounter() != null) {
                eq.setHourKmCounter(req.getFinalHourKmCounter());
            }
            equipmentService.changeStatus(eq, nextStatus,
                    "Demobilizasiya — qaraja qaytarıldı" + (req != null && req.getReturnNotes() != null ? ": " + req.getReturnNotes() : ""),
                    equipmentService.currentUserOrNull());
            equipmentRepository.save(eq);
        }

        String notes = req != null ? req.getReturnNotes() : null;
        Double counter = req != null && req.getFinalHourKmCounter() != null ? req.getFinalHourKmCounter().doubleValue() : null;
        workflowTelegramService.notifyProjectCompleted(p, counter, notes);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "QARAJA_QAYTARILDI", "Texnika qaraja təhvil verildi və sərbəstləşdirildi");

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

    // QEYD: updateEndDate silindi — bitmə tarixi yalnız layihə bağlananda (complete) qeyd olunur.

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

    // ─── İnsident, Dayanma və Texnika Əvəzləmə İdarəetməsi ───────────────────

    @Transactional
    public com.ces.erp.project.dto.ProjectDowntimeResponse pauseProject(Long id, com.ces.erp.project.dto.ProjectPauseRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Yalnız aktiv (ACTIVE) layihə dayandırıla bilər");
        }

        com.ces.erp.project.entity.ProjectDowntime dt = com.ces.erp.project.entity.ProjectDowntime.builder()
                .project(p)
                .startDate(req.getStartDate() != null ? req.getStartDate() : LocalDate.now())
                .reasonType(req.getReasonType())
                .reasonDescription(req.getReasonDescription())
                .isPaid(req.isPaid())
                .standbyRate(req.getStandbyRate())
                .autoExtendEndDate(req.isAutoExtendEndDate())
                .status("ACTIVE")
                .build();
        dt = downtimeRepository.save(dt);

        p.setStatus(ProjectStatus.PAUSED);
        projectRepository.save(p);

        workflowTelegramService.notifyProjectPaused(p, req.getReasonType(), req.getReasonDescription());
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "DURDURULDU",
                "Layihə müvəqqəti dayandırıldı: " + req.getReasonType());

        return com.ces.erp.project.dto.ProjectDowntimeResponse.from(dt);
    }

    @Transactional
    public ProjectResponse resumeProject(Long id, com.ces.erp.project.dto.ProjectResumeRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.PAUSED) {
            throw new BusinessException("Yalnız dayandırılmış (PAUSED) layihə bərpa edilə bilər");
        }

        LocalDate resumeDate = req.getResumeDate() != null ? req.getResumeDate() : LocalDate.now();
        List<com.ces.erp.project.entity.ProjectDowntime> activeDowntimes = downtimeRepository.findByProjectIdAndStatusAndDeletedFalse(id, "ACTIVE");

        long totalDaysPaused = 0;
        boolean shouldExtend = req.isAutoExtendEndDate();

        for (com.ces.erp.project.entity.ProjectDowntime dt : activeDowntimes) {
            dt.setEndDate(resumeDate);
            dt.setStatus("RESOLVED");
            dt.setResolvedNotes(req.getResolvedNotes());
            if (dt.getStartDate() != null && !resumeDate.isBefore(dt.getStartDate())) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(dt.getStartDate(), resumeDate);
                if (days > 0) totalDaysPaused += days;
            }
            if (dt.isAutoExtendEndDate()) shouldExtend = true;
            downtimeRepository.save(dt);
        }

        if (shouldExtend && totalDaysPaused > 0 && p.getEndDate() != null) {
            p.setEndDate(p.getEndDate().plusDays(totalDaysPaused));
        }

        p.setStatus(ProjectStatus.ACTIVE);
        projectRepository.save(p);

        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        workflowTelegramService.notifyProjectResumed(p, p.getEndDate());
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "BƏRPA_EDİLDİ",
                "Layihə bərpa edildi" + (totalDaysPaused > 0 ? ", bitmə tarixi " + totalDaysPaused + " gün uzadıldı" : ""));

        return ProjectResponse.from(p, plan);
    }

    @Transactional
    public com.ces.erp.project.dto.ProjectEquipmentSwapResponse swapEquipment(Long id, com.ces.erp.project.dto.ProjectEquipmentSwapRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() == ProjectStatus.COMPLETED || p.getStatus() == ProjectStatus.CANCELLED) {
            throw new BusinessException("Bağlanmış və ya ləğv edilmiş layihədə texnika dəyişdirilə bilməz");
        }

        Equipment oldEq = equipmentRepository.findById(req.getOldEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Köhnə Texnika", req.getOldEquipmentId()));
        Equipment newEq = equipmentRepository.findById(req.getNewEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Yeni Texnika", req.getNewEquipmentId()));

        if (newEq.getStatus() != com.ces.erp.enums.EquipmentStatus.AVAILABLE && newEq.getStatus() != com.ces.erp.enums.EquipmentStatus.IN_INSPECTION) {
            throw new BusinessException("Seçilmiş yeni texnika hazırda sərbəst (AVAILABLE) deyil: " + newEq.getStatus());
        }

        // 1. Köhnə texnikanın sayğacını yenilə və təmirə/servisə yönləndir
        if (req.getOldEquipmentFinalCounter() != null) {
            oldEq.setHourKmCounter(BigDecimal.valueOf(req.getOldEquipmentFinalCounter()));
        }
        com.ces.erp.enums.EquipmentStatus oldNextStatus = com.ces.erp.enums.EquipmentStatus.valueOf(
                req.getOldEquipmentNextStatus() != null ? req.getOldEquipmentNextStatus() : "IN_REPAIR"
        );
        equipmentService.changeStatus(oldEq, oldNextStatus,
                "Layihədən çıxarıldı və əvəzləndi (" + p.getProjectCode() + "): " + req.getSwapReason(),
                equipmentService.currentUserOrNull());
        equipmentRepository.save(oldEq);

        // 2. Yeni texnikanın ilkin sayğacını yaz və RENTED statusuna keçir
        if (req.getNewEquipmentInitialCounter() != null) {
            newEq.setHourKmCounter(BigDecimal.valueOf(req.getNewEquipmentInitialCounter()));
        }
        equipmentService.changeStatus(newEq, com.ces.erp.enums.EquipmentStatus.RENTED,
                "Layihəyə əvəzedici olaraq təyin edildi (" + p.getProjectCode() + ")",
                equipmentService.currentUserOrNull());
        equipmentRepository.save(newEq);

        // 3. Planda və Sorğuda texnika referanslarını yenilə
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        if (plan != null) {
            if (plan.getItems() != null && !plan.getItems().isEmpty()) {
                for (CoordinatorPlanItem it : plan.getItems()) {
                    if (it.getEquipment() != null && it.getEquipment().getId().equals(oldEq.getId())) {
                        it.setEquipment(newEq);
                    }
                }
            }
            if (plan.getSelectedEquipment() != null && plan.getSelectedEquipment().getId().equals(oldEq.getId())) {
                plan.setSelectedEquipment(newEq);
            }
            planRepository.save(plan);
        }
        if (p.getRequest() != null && p.getRequest().getSelectedEquipment() != null
                && p.getRequest().getSelectedEquipment().getId().equals(oldEq.getId())) {
            p.getRequest().setSelectedEquipment(newEq);
        }

        // 4. Əvəzləmə qeydiyyatı yarat
        com.ces.erp.project.entity.ProjectEquipmentSwap swap = com.ces.erp.project.entity.ProjectEquipmentSwap.builder()
                .project(p)
                .oldEquipment(oldEq)
                .oldEquipmentFinalCounter(req.getOldEquipmentFinalCounter())
                .oldEquipmentNextStatus(oldNextStatus.name())
                .newEquipment(newEq)
                .newEquipmentInitialCounter(req.getNewEquipmentInitialCounter())
                .swapDate(req.getSwapDate() != null ? req.getSwapDate() : LocalDate.now())
                .swapReason(req.getSwapReason())
                .notes(req.getNotes())
                .build();
        swap = swapRepository.save(swap);

        workflowTelegramService.notifyEquipmentSwapped(p, oldEq.getName(), newEq.getName(), req.getSwapReason());
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "TEXNİKA_ƏVƏZLƏNMƏSİ",
                "Texnika əvəzləndi: " + oldEq.getName() + " -> " + newEq.getName());

        return com.ces.erp.project.dto.ProjectEquipmentSwapResponse.from(swap);
    }

    @Transactional
    public ProjectResponse earlyTerminate(Long id, com.ces.erp.project.dto.ProjectEarlyTerminateRequest req) {
        Project p = findOrThrow(id);
        if (p.getStatus() == ProjectStatus.COMPLETED || p.getStatus() == ProjectStatus.CANCELLED) {
            throw new BusinessException("Bağlanmış və ya ləğv edilmiş layihə yenidən xitam edilə bilməz");
        }

        LocalDate termDate = req.getTerminationDate() != null ? req.getTerminationDate() : LocalDate.now();
        p.setEndDate(termDate);
        p.setStatus(ProjectStatus.CANCELLED);
        projectRepository.save(p);

        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);

        // Texnikaları sərbəstləşdir və ya servisə qaytar
        List<Equipment> targetEquipments = new java.util.ArrayList<>();
        if (plan != null && plan.getItems() != null && !plan.getItems().isEmpty()) {
            for (CoordinatorPlanItem it : plan.getItems()) {
                if (it.getEquipment() != null && !it.isDeleted()) targetEquipments.add(it.getEquipment());
            }
        }
        if (targetEquipments.isEmpty()) {
            Equipment eq = plan != null && plan.getSelectedEquipment() != null
                    ? plan.getSelectedEquipment()
                    : (p.getRequest() != null ? p.getRequest().getSelectedEquipment() : null);
            if (eq != null) targetEquipments.add(eq);
        }

        com.ces.erp.enums.EquipmentStatus nextStatus = req.isRequiresInspection()
                ? com.ces.erp.enums.EquipmentStatus.IN_INSPECTION
                : com.ces.erp.enums.EquipmentStatus.AVAILABLE;

        for (Equipment eq : targetEquipments) {
            if (req.getFinalHourKmCounter() != null) {
                eq.setHourKmCounter(BigDecimal.valueOf(req.getFinalHourKmCounter()));
            }
            equipmentService.changeStatus(eq, nextStatus,
                    "Vaxtından əvvəl xitam verildi (" + p.getProjectCode() + ")" + (req.getReturnNotes() != null ? ": " + req.getReturnNotes() : ""),
                    equipmentService.currentUserOrNull());
            equipmentRepository.save(eq);
        }

        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "VAXTINDAN_ƏVVƏL_XİTAM",
                "Layihə vaxtından əvvəl xitam verildi: " + req.getTerminationReason());

        return ProjectResponse.from(p, plan);
    }

    @Transactional(readOnly = true)
    public List<com.ces.erp.project.dto.ProjectDowntimeResponse> getDowntimes(Long projectId) {
        findOrThrow(projectId);
        return downtimeRepository.findByProjectIdAndDeletedFalseOrderByStartDateDesc(projectId).stream()
                .map(com.ces.erp.project.dto.ProjectDowntimeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.ces.erp.project.dto.ProjectEquipmentSwapResponse> getEquipmentSwaps(Long projectId) {
        findOrThrow(projectId);
        return swapRepository.findByProjectIdAndDeletedFalseOrderBySwapDateDesc(projectId).stream()
                .map(com.ces.erp.project.dto.ProjectEquipmentSwapResponse::from)
                .toList();
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findByIdWithFinances(id)
                .orElseThrow(() -> new ResourceNotFoundException("Layihə", id));
    }
}
