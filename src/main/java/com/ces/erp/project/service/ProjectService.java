package com.ces.erp.project.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.ProjectStatus;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectExpenseRepository expenseRepository;
    private final ProjectRevenueRepository revenueRepository;
    private final CoordinatorPlanRepository planRepository;
    private final FileStorageService fileStorageService;

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
    public ProjectResponse getById(Long id) {
        Project p = findOrThrow(id);
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    // ─── Müqavilə upload ──────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse uploadContract(Long id, MultipartFile file) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.PENDING) {
            throw new BusinessException("Müqavilə yalnız PENDING statuslu layihəyə yüklənə bilər");
        }

        String path = fileStorageService.store(file, "project-contracts");
        p.setContractFilePath(path);
        p.setContractFileName(file.getOriginalFilename());
        p.setHasContract(true);
        p.setStatus(ProjectStatus.ACTIVE);
        if (p.getStartDate() == null) {
            p.setStartDate(LocalDate.now());
        }

        projectRepository.save(p);
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
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
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("Bağlanmış layihəyə xərc əlavə edilə bilməz");
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
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("Bağlanmış layihəyə gəlir əlavə edilə bilməz");
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

        p.setEvacuationCost(req.getEvacuationCost());
        p.setScheduledHours(req.getScheduledHours());
        p.setActualHours(req.getActualHours());
        p.setStatus(ProjectStatus.COMPLETED);
        if (p.getEndDate() == null) {
            p.setEndDate(LocalDate.now());
        }

        projectRepository.save(p);
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    // ─── Bitmə tarixini yenilə ────────────────────────────────────────────────

    @Transactional
    public ProjectResponse updateEndDate(Long id, LocalDate endDate) {
        Project p = findOrThrow(id);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Bitmə tarixi yalnız ACTIVE layihədə dəyişdirilə bilər");
        }
        p.setEndDate(endDate);
        projectRepository.save(p);
        CoordinatorPlan plan = planRepository.findByRequestId(p.getRequest().getId()).orElse(null);
        return ProjectResponse.from(p, plan);
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findByIdWithFinances(id)
                .orElseThrow(() -> new ResourceNotFoundException("Layihə", id));
    }
}
