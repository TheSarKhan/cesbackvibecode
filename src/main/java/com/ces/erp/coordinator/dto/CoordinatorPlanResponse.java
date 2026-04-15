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

    // Müştəri məlumatları
    private Long customerId;
    private String customerVoen;
    private String customerAddress;
    private String customerSupplierPerson;
    private String customerSupplierPhone;
    private String customerOfficeContactPerson;
    private String customerOfficeContactPhone;
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
    private List<String> equipmentDocumentTypes;

    // Təsdiq vəziyyəti
    private boolean hasPendingSubmit;

    // Plan məlumatları
    private Long planId;
    private Long operatorId;
    private String operatorName;
    private BigDecimal equipmentPrice;
    private BigDecimal contractorDailyRate;
    private BigDecimal contractorPayment;
    private BigDecimal operatorPayment;
    private BigDecimal transportationPrice;
    private BigDecimal totalAmount;
    private BigDecimal companyProfit;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<SafetyItemDto> safetyEquipment;
    private String notes;
    private List<DocumentDto> documents;
    private LocalDateTime planCreatedAt;

    @Data
    @Builder
    public static class SafetyItemDto {
        private Long id;
        private String name;
    }

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
        var customer = r.getCustomer();
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
                .customerId(customer != null ? customer.getId() : null)
                .customerVoen(customer != null ? customer.getVoen() : null)
                .customerAddress(customer != null ? customer.getAddress() : null)
                .customerSupplierPerson(customer != null ? customer.getSupplierPerson() : null)
                .customerSupplierPhone(customer != null ? customer.getSupplierPhone() : null)
                .customerOfficeContactPerson(customer != null ? customer.getOfficeContactPerson() : null)
                .customerOfficeContactPhone(customer != null ? customer.getOfficeContactPhone() : null)
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
            base.setEquipmentDocumentTypes(
                eq.getDocuments() != null
                    ? eq.getDocuments().stream()
                        .map(d -> d.getDocumentType())
                        .filter(t -> t != null && !t.isBlank())
                        .distinct()
                        .toList()
                    : List.of()
            );
        }

        BigDecimal eqPrice = plan.getEquipmentPrice() != null ? plan.getEquipmentPrice() : BigDecimal.ZERO;
        BigDecimal transPrice = plan.getTransportationPrice() != null ? plan.getTransportationPrice() : BigDecimal.ZERO;
        BigDecimal contrPayment = plan.getContractorPayment() != null ? plan.getContractorPayment() : BigDecimal.ZERO;
        BigDecimal opPayment = plan.getOperatorPayment() != null ? plan.getOperatorPayment() : BigDecimal.ZERO;
        BigDecimal total = eqPrice.add(transPrice);
        BigDecimal profit = total.subtract(contrPayment).subtract(opPayment);

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
        base.setOperatorId(plan.getOperator() != null ? plan.getOperator().getId() : null);
        base.setOperatorName(plan.getOperator() != null
                ? plan.getOperator().getFirstName() + " " + plan.getOperator().getLastName()
                : null);
        // Planın öz dayCount-u varsa onu göstər, yoxsa sorğudan gələni saxla
        if (plan.getDayCount() != null) {
            base.setDayCount(plan.getDayCount());
        }
        base.setEquipmentPrice(plan.getEquipmentPrice());
        base.setContractorDailyRate(plan.getContractorDailyRate());
        base.setContractorPayment(plan.getContractorPayment());
        base.setOperatorPayment(plan.getOperatorPayment());
        base.setTransportationPrice(plan.getTransportationPrice());
        base.setTotalAmount(total);
        base.setCompanyProfit(profit);
        base.setStartDate(plan.getStartDate());
        base.setEndDate(plan.getEndDate());
        base.setSafetyEquipment(plan.getSafetyEquipment().stream()
                .map(s -> SafetyItemDto.builder().id(s.getId()).name(s.getKey()).build())
                .toList());
        base.setNotes(plan.getNotes());
        base.setDocuments(docs);
        base.setPlanCreatedAt(plan.getCreatedAt());
        return base;
    }
}
