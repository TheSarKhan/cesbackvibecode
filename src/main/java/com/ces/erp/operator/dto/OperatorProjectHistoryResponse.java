package com.ces.erp.operator.dto;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.project.entity.Project;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OperatorProjectHistoryResponse {

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
    private BigDecimal operatorPayment;
    private String equipmentName;
    private String equipmentCode;

    public static OperatorProjectHistoryResponse from(Project p, CoordinatorPlan plan) {
        var r = p.getRequest();
        var eq = plan.getSelectedEquipment() != null ? plan.getSelectedEquipment()
                : (r != null ? r.getSelectedEquipment() : null);

        return OperatorProjectHistoryResponse.builder()
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
                .operatorPayment(plan.getOperatorPayment())
                .equipmentName(eq != null ? eq.getName() : null)
                .equipmentCode(eq != null ? eq.getEquipmentCode() : null)
                .build();
    }
}
