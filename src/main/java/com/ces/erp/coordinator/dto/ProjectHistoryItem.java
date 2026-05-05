package com.ces.erp.coordinator.dto;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.project.entity.Project;
import com.ces.erp.request.entity.TechRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProjectHistoryItem {

    private Long projectId;
    private String projectCode;
    private String requestCode;
    private String companyName;
    private String projectName;
    private String region;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayCount;

    private String equipmentName;
    private String equipmentCode;
    private String equipmentType;

    private BigDecimal contractorPayment;
    private BigDecimal contractorDailyRate;
    private BigDecimal equipmentPriceTotal;
    private String operatorName;

    public static ProjectHistoryItem from(Project p, CoordinatorPlan plan) {
        TechRequest r = p.getRequest();
        Equipment eq = plan.getSelectedEquipment() != null ? plan.getSelectedEquipment()
                : (r != null ? r.getSelectedEquipment() : null);

        BigDecimal equipmentPriceTotal = null;
        if (plan.getEquipmentPrice() != null) {
            int days = plan.getDayCount() != null ? plan.getDayCount() : 0;
            equipmentPriceTotal = days > 0
                    ? plan.getEquipmentPrice().multiply(BigDecimal.valueOf(days))
                    : plan.getEquipmentPrice();
        }

        String operatorName = null;
        if (plan.getOperator() != null) {
            operatorName = plan.getOperator().getFirstName() + " " + plan.getOperator().getLastName();
        }

        return ProjectHistoryItem.builder()
                .projectId(p.getId())
                .projectCode(p.getProjectCode())
                .requestCode(r != null ? "REQ-" + String.format("%04d", r.getId()) : null)
                .companyName(r != null ? r.getCompanyName() : null)
                .projectName(r != null ? r.getProjectName() : null)
                .region(r != null ? r.getRegion() : null)
                .status(p.getStatus())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .dayCount(plan.getDayCount())
                .equipmentName(eq != null ? eq.getName() : null)
                .equipmentCode(eq != null ? eq.getEquipmentCode() : null)
                .equipmentType(eq != null ? eq.getType() : null)
                .contractorPayment(plan.getContractorPayment())
                .contractorDailyRate(plan.getContractorDailyRate())
                .equipmentPriceTotal(equipmentPriceTotal)
                .operatorName(operatorName)
                .build();
    }
}
