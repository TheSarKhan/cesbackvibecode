package com.ces.erp.project.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.entity.EquipmentProjectHistory;
import com.ces.erp.garage.repository.EquipmentProjectHistoryRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.project.dto.FinanceEntryRequest;
import com.ces.erp.project.dto.ProjectCompleteRequest;
import com.ces.erp.project.dto.ProjectResponse;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.entity.ProjectExpense;
import com.ces.erp.project.entity.ProjectRevenue;
import com.ces.erp.project.repository.ProjectExpenseRepository;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.project.repository.ProjectRevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectExpenseRepository expenseRepository;
    private final ProjectRevenueRepository revenueRepository;
    private final CoordinatorPlanRepository planRepository;
    private final EquipmentProjectHistoryRepository equipmentHistoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

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

    // ─── Müqavilə upload ──────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse uploadContract(Long id, MultipartFile file, LocalDate startDate) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.PENDING) {
            throw new BusinessException("Müqavilə yalnız PENDING statuslu layihəyə yüklənə bilər");
        }

        String path = fileStorageService.store(file, "project-contracts");
        p.setContractFilePath(path);
        p.setContractFileName(file.getOriginalFilename());
        p.setHasContract(true);
        p.setStatus(ProjectStatus.ACTIVE);
        p.setStartDate(startDate != null ? startDate : LocalDate.now());

        projectRepository.save(p);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "YARADILDI", "Yeni layihə yaradıldı");
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    // ─── Müqavilə yüklə ──────────────────────────────────────────────────────

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

        // Planlaşdırılan saatlar: dayCount × 9 (1 gün = 9 saat)
        CoordinatorPlan planForHours = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        Integer planDayCount = planForHours != null && planForHours.getDayCount() != null
                ? planForHours.getDayCount()
                : (p.getRequest() != null ? p.getRequest().getDayCount() : null);
        BigDecimal scheduled = planDayCount != null
                ? BigDecimal.valueOf(planDayCount).multiply(BigDecimal.valueOf(9))
                : BigDecimal.ZERO;

        BigDecimal actual = req.getActualHours();
        BigDecimal overtimeRate = req.getOvertimeRate() != null ? req.getOvertimeRate() : BigDecimal.ONE;
        BigDecimal overtimeHours = actual.subtract(scheduled).max(BigDecimal.ZERO);

        // Əlavə vaxt haqqı: overtimeHours × (günlük qiymət / 9) × overtimeRate
        BigDecimal equipmentPrice = planForHours != null && planForHours.getEquipmentPrice() != null
                ? planForHours.getEquipmentPrice() : BigDecimal.ZERO;
        BigDecimal dailyRate = planDayCount != null && planDayCount > 0
                ? equipmentPrice.divide(BigDecimal.valueOf(planDayCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal hourlyRate = dailyRate.divide(BigDecimal.valueOf(9), 4, RoundingMode.HALF_UP);
        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(overtimeRate).setScale(2, RoundingMode.HALF_UP);

        p.setEvacuationCost(req.getEvacuationCost());
        p.setScheduledHours(scheduled);
        p.setActualHours(actual);
        p.setOvertimeHours(overtimeHours);
        p.setOvertimeRate(overtimeRate);
        p.setOvertimePay(overtimePay);
        p.setStatus(ProjectStatus.COMPLETED);
        if (p.getEndDate() == null) {
            p.setEndDate(LocalDate.now());
        }

        projectRepository.save(p);
        auditService.log("LAYİHƏ", p.getId(), p.getProjectCode(), "YENİLƏNDİ", "Layihə tamamlandı");
        CoordinatorPlan plan = planForHours;

        // Texnikanın layihə tarixçəsinə qeyd yaz
        Equipment eq = plan != null && plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : (p.getRequest() != null ? p.getRequest().getSelectedEquipment() : null);
        if (eq != null) {
            EquipmentProjectHistory history = EquipmentProjectHistory.builder()
                    .equipment(eq)
                    .projectId(p.getId())
                    .projectName(p.getRequest() != null ? p.getRequest().getProjectName() : p.getProjectCode())
                    .startDate(p.getStartDate())
                    .endDate(p.getEndDate())
                    .contractorRevenue(plan != null && plan.getContractorPayment() != null
                            ? plan.getContractorPayment() : BigDecimal.ZERO)
                    .status("COMPLETED")
                    .notes(p.getRequest() != null ? p.getRequest().getCompanyName() : null)
                    .build();
            equipmentHistoryRepository.save(history);

            // Texnikanı avtomatik "Yolda" statusuna keçir
            if (eq.getStatus() == EquipmentStatus.RENTED) {
                eq.setStatus(EquipmentStatus.IN_TRANSIT);
                equipmentRepository.save(eq);
            }
        }

        return ProjectResponse.from(p, plan);
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

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findByIdWithFinances(id)
                .orElseThrow(() -> new ResourceNotFoundException("Layihə", id));
    }
}
