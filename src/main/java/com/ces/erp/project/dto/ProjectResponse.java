package com.ces.erp.project.dto;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.entity.ProjectExpense;
import com.ces.erp.project.entity.ProjectRevenue;
import com.ces.erp.request.entity.TechRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProjectResponse {

    private Long id;
    private String projectCode;
    private String requestCode;

    // Müştəri / layihə
    private String companyName;
    private String contactPerson;
    private String contactPhone;
    private String projectName;
    private String region;
    private ProjectType projectType;
    private Integer dayCount;

    // Texnika / mənbə
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;
    private String ownershipType;
    private String contractorName;
    private BigDecimal contractorAmount;

    // Tarixlər
    private LocalDate startDate;
    private LocalDate endDate;

    // Status
    private ProjectStatus status;
    private boolean hasContract;

    // Maliyyə
    private BigDecimal totalExpense;
    private BigDecimal totalRevenue;
    private BigDecimal netProfit;

    // Bağlanış
    private BigDecimal evacuationCost;
    private BigDecimal scheduledHours;
    private BigDecimal actualHours;

    // ─── Builder method ────────────────────────────────────────────────────────

    public static ProjectResponse from(Project p, CoordinatorPlan plan) {
        TechRequest r = p.getRequest();

        // Texnika
        Equipment eq = plan != null && plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : (r != null ? r.getSelectedEquipment() : null);

        BigDecimal contractorAmount = plan != null ? plan.getContractorPayment() : BigDecimal.ZERO;

        // Maliyyə
        BigDecimal totalExpense = p.getExpenses().stream()
                .filter(e -> !e.isDeleted())
                .map(ProjectExpense::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = p.getRevenues().stream()
                .filter(rv -> !rv.isDeleted())
                .map(ProjectRevenue::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        return ProjectResponse.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .requestCode(r != null ? "REQ-" + String.format("%04d", r.getId()) : null)
                .companyName(r != null ? r.getCompanyName() : null)
                .contactPerson(r != null ? r.getContactPerson() : null)
                .contactPhone(r != null ? r.getContactPhone() : null)
                .projectName(r != null ? r.getProjectName() : null)
                .region(r != null ? r.getRegion() : null)
                .projectType(r != null ? r.getProjectType() : null)
                .dayCount(r != null ? r.getDayCount() : null)
                .equipmentId(eq != null ? eq.getId() : null)
                .equipmentName(eq != null ? eq.getName() : null)
                .equipmentCode(eq != null ? eq.getEquipmentCode() : null)
                .ownershipType(eq != null ? eq.getOwnershipType().name() : null)
                .contractorName(eq != null && eq.getOwnerContractor() != null ? eq.getOwnerContractor().getCompanyName() : null)
                .contractorAmount(contractorAmount)
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .hasContract(p.isHasContract())
                .totalExpense(totalExpense)
                .totalRevenue(totalRevenue)
                .netProfit(netProfit)
                .evacuationCost(p.getEvacuationCost())
                .scheduledHours(p.getScheduledHours())
                .actualHours(p.getActualHours())
                .build();
    }

    // ─── Finance nested DTOs ───────────────────────────────────────────────────

    @Data
    @Builder
    public static class FinanceEntryDto {
        private Long id;
        private String key;
        private BigDecimal value;
        private LocalDate date;
    }

    @Data
    @Builder
    public static class FinancesDto {
        private List<FinanceEntryDto> expenses;
        private List<FinanceEntryDto> revenues;
    }
}
