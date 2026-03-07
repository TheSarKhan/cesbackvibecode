package com.ces.erp.coordinator.dto;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.request.entity.TechRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CoordinatorPlanResponse {

    // Sorğu məlumatları (readonly)
    private Long requestId;
    private String requestCode;
    private String companyName;
    private String contactPerson;
    private String contactPhone;
    private String projectName;
    private String region;
    private ProjectType projectType;
    private Integer dayCount;
    private boolean transportationRequired;
    private RequestStatus requestStatus;
    private List<ParamDto> params;

    // Koordinatorun seçdiyi texnika
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;
    private String ownershipType;
    private String contractorName;

    // Plan məlumatları
    private Long planId;
    private String operatorName;
    private BigDecimal equipmentPrice;
    private BigDecimal contractorPayment;
    private BigDecimal transportationPrice;
    private BigDecimal totalAmount;
    private BigDecimal companyProfit;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean hasFlashingLights;
    private boolean hasFireExtinguisher;
    private boolean hasFirstAid;
    private String notes;
    private List<DocumentDto> documents;
    private LocalDateTime planCreatedAt;

    @Data
    @Builder
    public static class ParamDto {
        private String paramKey;
        private String paramValue;
    }

    @Data
    @Builder
    public static class DocumentDto {
        private Long id;
        private String documentName;
        private String fileType;
        private String documentType;
        private String uploadedByName;
        private LocalDateTime uploadedAt;
    }

    public static CoordinatorPlanResponse fromRequest(TechRequest r) {
        List<ParamDto> params = r.getParams() == null ? List.of() :
                r.getParams().stream()
                        .map(p -> ParamDto.builder().paramKey(p.getParamKey()).paramValue(p.getParamValue()).build())
                        .toList();
        return CoordinatorPlanResponse.builder()
                .requestId(r.getId())
                .requestCode("REQ-" + String.format("%04d", r.getId()))
                .companyName(r.getCompanyName())
                .contactPerson(r.getContactPerson())
                .contactPhone(r.getContactPhone())
                .projectName(r.getProjectName())
                .region(r.getRegion())
                .projectType(r.getProjectType())
                .dayCount(r.getDayCount())
                .transportationRequired(r.isTransportationRequired())
                .requestStatus(r.getStatus())
                .params(params)
                .build();
    }

    public static CoordinatorPlanResponse from(CoordinatorPlan plan) {
        TechRequest r = plan.getRequest();
        CoordinatorPlanResponse base = fromRequest(r);

        // Koordinatorun seçdiyi texnika (prioritet), əks halda sorğudan gələn texnika
        Equipment eq = plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : r.getSelectedEquipment();
        if (eq != null) {
            base.setEquipmentId(eq.getId());
            base.setEquipmentName(eq.getName());
            base.setEquipmentCode(eq.getEquipmentCode());
            base.setOwnershipType(eq.getOwnershipType().name());
            base.setContractorName(eq.getOwnerContractor() != null ? eq.getOwnerContractor().getCompanyName() : null);
        }

        BigDecimal eqPrice = plan.getEquipmentPrice() != null ? plan.getEquipmentPrice() : BigDecimal.ZERO;
        BigDecimal transPrice = plan.getTransportationPrice() != null ? plan.getTransportationPrice() : BigDecimal.ZERO;
        BigDecimal contrPayment = plan.getContractorPayment() != null ? plan.getContractorPayment() : BigDecimal.ZERO;
        BigDecimal total = eqPrice.add(transPrice);
        BigDecimal profit = total.subtract(contrPayment);

        List<DocumentDto> docs = plan.getDocuments().stream()
                .filter(d -> !d.isDeleted())
                .map(d -> DocumentDto.builder()
                        .id(d.getId())
                        .documentName(d.getDocumentName())
                        .fileType(d.getFileType())
                        .documentType(d.getDocumentType())
                        .uploadedByName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : null)
                        .uploadedAt(d.getCreatedAt())
                        .build())
                .toList();

        base.setPlanId(plan.getId());
        base.setOperatorName(plan.getOperatorName());
        base.setEquipmentPrice(plan.getEquipmentPrice());
        base.setContractorPayment(plan.getContractorPayment());
        base.setTransportationPrice(plan.getTransportationPrice());
        base.setTotalAmount(total);
        base.setCompanyProfit(profit);
        base.setStartDate(plan.getStartDate());
        base.setEndDate(plan.getEndDate());
        base.setHasFlashingLights(plan.isHasFlashingLights());
        base.setHasFireExtinguisher(plan.isHasFireExtinguisher());
        base.setHasFirstAid(plan.isHasFirstAid());
        base.setNotes(plan.getNotes());
        base.setDocuments(docs);
        base.setPlanCreatedAt(plan.getCreatedAt());
        return base;
    }
}
