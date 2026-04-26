package com.ces.erp.project.dto;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
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
public class
ProjectResponse {

    private Long id;
    private String projectCode;
    private String requestCode;

    // ─── Müştəri / layihə ─────────────────────────────────────────────────────
    private String companyName;
    private String contactPerson;
    private String contactPhone;
    private String projectName;
    private String region;
    private ProjectType projectType;
    private Integer dayCount;
    private boolean transportationRequired;
    private LocalDate requestDate;

    // ─── Sorğunun texniki parametrləri ────────────────────────────────────────
    private List<TechParamDto> requestParams;

    // ─── Texnika ──────────────────────────────────────────────────────────────
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private String equipmentBrand;
    private String equipmentModel;
    private String equipmentSerialNumber;
    private String equipmentPlateNumber;
    private String ownershipType;

    // ─── Podratçı (CONTRACTOR texnikası üçün) ─────────────────────────────────
    private String contractorName;
    private String contractorVoen;
    private String contractorPhone;
    private String contractorContactPerson;
    private BigDecimal contractorDailyRate;
    private BigDecimal contractorPayment;

    // ─── İnvestor (INVESTOR texnikası üçün) ───────────────────────────────────
    private String investorName;
    private String investorVoen;
    private String investorPhone;

    // ─── Koordinator planı ────────────────────────────────────────────────────
    private BigDecimal planEquipmentPrice;       // vahid qiymət (gündəlik / aylıq)
    private BigDecimal planEquipmentTotal;       // cəmi texnika qiyməti (DAILY: price×days, MONTHLY: price)
    private BigDecimal planTransportationPrice;
    private BigDecimal planOperatorPayment;
    private Integer planDayCount;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private String operatorName;
    private String planNotes;

    // ─── Tarixlər ─────────────────────────────────────────────────────────────
    private LocalDate startDate;
    private LocalDate endDate;

    // ─── Status / Müqavilə ────────────────────────────────────────────────────
    private ProjectStatus status;
    private boolean hasContract;
    private String contractFileName;

    // ─── Maliyyə cəmi ─────────────────────────────────────────────────────────
    private BigDecimal totalExpense;
    private BigDecimal totalRevenue;
    private BigDecimal netProfit;

    // ─── Bağlanış ─────────────────────────────────────────────────────────────
    private BigDecimal evacuationCost;
    private BigDecimal scheduledHours;
    private BigDecimal actualHours;
    private BigDecimal overtimeHours;
    private BigDecimal overtimeRate;
    private BigDecimal overtimePay;

    // ─── Builder method ───────────────────────────────────────────────────────

    public static ProjectResponse from(Project p, CoordinatorPlan plan) {
        TechRequest r = p.getRequest();

        Equipment eq = plan != null && plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : (r != null ? r.getSelectedEquipment() : null);

        BigDecimal totalExpense = p.getExpenses().stream()
                .filter(e -> !e.isDeleted())
                .map(ProjectExpense::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = p.getRevenues().stream()
                .filter(rv -> !rv.isDeleted())
                .map(ProjectRevenue::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Texnika mənbəyinə görə contractor / investor məlumatları
        String contractorName = null, contractorVoen = null, contractorPhone = null, contractorContactPerson = null;
        String investorName = null, investorVoen = null, investorPhone = null;

        if (eq != null) {
            switch (eq.getOwnershipType()) {
                case CONTRACTOR -> {
                    if (eq.getOwnerContractor() != null) {
                        contractorName = eq.getOwnerContractor().getCompanyName();
                        contractorVoen = eq.getOwnerContractor().getVoen();
                        contractorPhone = eq.getOwnerContractor().getPhone();
                        contractorContactPerson = eq.getOwnerContractor().getContactPerson();
                    }
                }
                case INVESTOR -> {
                    investorName = eq.getOwnerInvestorName();
                    investorVoen = eq.getOwnerInvestorVoen();
                    investorPhone = eq.getOwnerInvestorPhone();
                }
                default -> {}
            }
        }

        return ProjectResponse.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .requestCode(r != null ? "REQ-" + String.format("%04d", r.getId()) : null)
                // Müştəri
                .companyName(r != null ? r.getCompanyName() : null)
                .contactPerson(r != null ? r.getContactPerson() : null)
                .contactPhone(r != null ? r.getContactPhone() : null)
                .projectName(r != null ? r.getProjectName() : null)
                .region(r != null ? r.getRegion() : null)
                .projectType(r != null ? r.getProjectType() : null)
                .dayCount(r != null ? r.getDayCount() : null)
                .transportationRequired(r != null && r.isTransportationRequired())
                .requestDate(r != null ? r.getRequestDate() : null)
                .requestParams(r != null ? r.getParams().stream()
                        .map(tp -> TechParamDto.builder().key(tp.getParamKey()).value(tp.getParamValue()).build())
                        .toList() : List.of())
                // Texnika
                .equipmentId(eq != null ? eq.getId() : null)
                .equipmentCode(eq != null ? eq.getEquipmentCode() : null)
                .equipmentName(eq != null ? eq.getName() : null)
                .equipmentType(eq != null ? eq.getType() : null)
                .equipmentBrand(eq != null ? eq.getBrand() : null)
                .equipmentModel(eq != null ? eq.getModel() : null)
                .equipmentSerialNumber(eq != null ? eq.getSerialNumber() : null)
                .equipmentPlateNumber(eq != null ? eq.getPlateNumber() : null)
                .ownershipType(eq != null ? eq.getOwnershipType().name() : null)
                // Podratçı / İnvestor
                .contractorName(contractorName)
                .contractorVoen(contractorVoen)
                .contractorPhone(contractorPhone)
                .contractorContactPerson(contractorContactPerson)
                .contractorDailyRate(plan != null ? plan.getContractorDailyRate() : null)
                .contractorPayment(plan != null ? plan.getContractorPayment() : BigDecimal.ZERO)
                .investorName(investorName)
                .investorVoen(investorVoen)
                .investorPhone(investorPhone)
                // Koordinator planı
                .planEquipmentPrice(plan != null ? plan.getEquipmentPrice() : null)
                .planEquipmentTotal(plan != null ? computeEquipmentTotal(plan, r) : null)
                .planTransportationPrice(plan != null ? plan.getTransportationPrice() : null)
                .planOperatorPayment(plan != null ? plan.getOperatorPayment() : null)
                .planDayCount(plan != null ? plan.getDayCount() : null)
                .planStartDate(plan != null ? plan.getStartDate() : null)
                .planEndDate(plan != null ? plan.getEndDate() : null)
                .operatorName(plan != null && plan.getOperator() != null
                        ? plan.getOperator().getFirstName() + " " + plan.getOperator().getLastName() : null)
                .planNotes(plan != null ? plan.getNotes() : null)
                // Tarixlər
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                // Status
                .status(p.getStatus())
                .hasContract(p.isHasContract())
                .contractFileName(p.getContractFileName())
                // Maliyyə
                .totalExpense(totalExpense)
                .totalRevenue(totalRevenue)
                .netProfit(totalRevenue.subtract(totalExpense))
                // Bağlanış
                .evacuationCost(p.getEvacuationCost())
                .scheduledHours(p.getScheduledHours())
                .actualHours(p.getActualHours())
                .overtimeHours(p.getOvertimeHours())
                .overtimeRate(p.getOvertimeRate())
                .overtimePay(p.getOvertimePay())
                .build();
    }

    private static BigDecimal computeEquipmentTotal(CoordinatorPlan plan, TechRequest r) {
        BigDecimal unitPrice = plan.getEquipmentPrice() != null ? plan.getEquipmentPrice() : BigDecimal.ZERO;
        int days = plan.getDayCount() != null ? plan.getDayCount() : 0;
        ProjectType type = r != null ? r.getProjectType() : null;
        if (type == ProjectType.MONTHLY || days == 0) {
            return unitPrice;
        }
        return unitPrice.multiply(BigDecimal.valueOf(days));
    }

    // ─── Nested DTOs ──────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class TechParamDto {
        private String key;
        private String value;
    }

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
