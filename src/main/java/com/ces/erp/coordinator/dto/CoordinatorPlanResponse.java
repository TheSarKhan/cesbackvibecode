package com.ces.erp.coordinator.dto;

import com.ces.erp.config.entity.ConfigItem;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private BigDecimal equipmentPrice;           // bizim podratçı/investora ödəyəcəyimiz (cost)
    private BigDecimal customerEquipmentPrice;   // sifarişçiyə təklif (revenue)
    private BigDecimal contractorDailyRate;
    private BigDecimal contractorPayment;
    private BigDecimal operatorPayment;
    private BigDecimal transportationPrice;
    private Long transportContractorId;
    private String transportContractorName;
    private BigDecimal totalAmount;
    private BigDecimal companyProfit;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<SafetyItemDto> safetyEquipment;
    private String notes;
    private List<DocumentDto> documents;
    private LocalDateTime planCreatedAt;

    // Mərhələ B (icra) status sahələri
    private boolean equipmentDocsVerified;
    private LocalDateTime equipmentDocsCheckedAt;
    // Yoxlama checklist-i: texnikanın məcburi + LM-in əlavə sənədləri (işarə vəziyyəti ilə)
    private List<RequiredDocDto> requiredDocuments;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private String deliveryNotes;

    // Qalib shortlist sətri (PM-də göstərilir)
    private Long winnerItemId;
    private String winnerPartyType;       // CONTRACTOR / INVESTOR
    private String winnerPartyName;       // Podratçı və ya İnvestor adı
    private String winnerEquipmentName;
    private String winnerEquipmentCode;

    // Shortlist sətirləri (koordinator UI-da göstərmək üçün — PM tərəfindən yaradılır)
    private List<ShortlistRowDto> shortlistItems;

    // Layihəyə seçilmiş texnika xətləri (çoxlu model)
    private List<PlanItemDto> items;

    @Data
    @Builder
    public static class PlanItemDto {
        private Long id;
        private Long shortlistItemId;
        private String partyType;
        private String partyName;
        private Long equipmentId;
        private String equipmentName;
        private String equipmentCode;
        private BigDecimal equipmentPrice;
        private BigDecimal customerEquipmentPrice;
        private BigDecimal transportationPrice;
        // Sifarişçi ilə razılaşma (PM mərhələsi — hər xətt ayrıca)
        private BigDecimal agreedEquipmentPrice;
        private BigDecimal agreedTransportPrice;
        private BigDecimal agreedTotalPrice;
        private String agreementNote;
        private Integer dayCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long operatorId;
        private String operatorName;
        private boolean equipmentDocsVerified;
        private LocalDateTime equipmentDocsCheckedAt;
        private LocalDateTime dispatchedAt;
        private LocalDateTime deliveredAt;
        private String deliveryNotes;
        private java.util.Set<Long> checkedDocumentItemIds;
        // İcra üçün: bu xəttin yoxlama checklist-i + təhvil-təslim aktı
        private List<RequiredDocDto> requiredDocuments;
        private Long actDocumentId;
        private String actFileName;
        // Texnikanın qarajda yüklənmiş faktiki sənədləri (koordinator baxış/yoxlaması üçün)
        private List<EquipmentDocDto> equipmentDocuments;
        // Müqavilə sənədləri (müştəri + sahib tərəfi) — koordinator oxu-rejimi
        private List<AgreementDocDto> agreementDocuments;
    }

    @Data
    @Builder
    public static class EquipmentDocDto {
        private Long id;
        private String name;
        private String type;
    }

    @Data
    @Builder
    public static class AgreementDocDto {
        private Long id;
        private String docType;     // CONTRACT | PRICE_PROTOCOL | OWNER_CONTRACT | OWNER_PRICE_PROTOCOL
        private String fileName;
    }

    @Data
    @Builder
    public static class ShortlistRowDto {
        private Long id;
        private String partyType;        // CONTRACTOR / INVESTOR
        private Long contractorId;
        private String contractorName;
        private String contractorVoen;
        private String contractorPhone;
        private String contractorContactPerson;
        private String contractorAddress;
        private Long investorId;
        private String investorName;
        private String investorVoen;
        private String investorPhone;
        private String investorContactPerson;
        private String investorAddress;
        private Long equipmentId;
        private String equipmentName;
        private String equipmentCode;
        private String equipmentType;
        private String equipmentBrand;
        private String equipmentModel;
        private Integer equipmentYear;
        private String equipmentPlateNumber;
        private String equipmentOwnership;
        private BigDecimal negotiatedPrice;
        private Integer rank;
        private String notes;
    }

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

    @Data
    @Builder
    public static class RequiredDocDto {
        private Long id;
        private String name;
        private boolean checked;
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

        // Yeni model — qiymətlər vahid başına (günlük/aylıq). Daşınma birdəfəlikdir.
        BigDecimal eqCostUnit    = plan.getEquipmentPrice() != null ? plan.getEquipmentPrice() : BigDecimal.ZERO;
        BigDecimal eqRevenueUnit = plan.getCustomerEquipmentPrice() != null ? plan.getCustomerEquipmentPrice() : BigDecimal.ZERO;
        BigDecimal transPrice    = plan.getTransportationPrice() != null ? plan.getTransportationPrice() : BigDecimal.ZERO;

        int units = plan.getDayCount() != null && plan.getDayCount() > 0 ? plan.getDayCount() : 1;
        BigDecimal multiplier = BigDecimal.valueOf(units);

        BigDecimal eqRevenueTotal = eqRevenueUnit.multiply(multiplier);
        BigDecimal eqCostTotal    = eqCostUnit.multiply(multiplier);

        BigDecimal total  = eqRevenueTotal.add(transPrice);     // sifarişçiyə təklif (cəm)
        BigDecimal profit = total.subtract(eqCostTotal);         // şirkət xeyri (cəm)

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
        base.setCustomerEquipmentPrice(plan.getCustomerEquipmentPrice());
        base.setContractorDailyRate(plan.getContractorDailyRate());
        base.setContractorPayment(plan.getContractorPayment());
        base.setOperatorPayment(plan.getOperatorPayment());
        base.setTransportationPrice(plan.getTransportationPrice());
        if (plan.getTransportContractor() != null) {
            base.setTransportContractorId(plan.getTransportContractor().getId());
            base.setTransportContractorName(plan.getTransportContractor().getCompanyName());
        }
        base.setEquipmentDocsVerified(plan.isEquipmentDocsVerified());
        base.setEquipmentDocsCheckedAt(plan.getEquipmentDocsCheckedAt());

        // Yoxlama checklist-i: texnikanın məcburi sənədləri ∪ LM-in əlavə sənədləri
        Set<Long> checkedIds = plan.getCheckedDocumentItemIds() != null
                ? plan.getCheckedDocumentItemIds() : Set.of();
        Map<Long, String> reqDocMap = new LinkedHashMap<>();
        if (eq != null && eq.getRequiredDocuments() != null) {
            for (ConfigItem ci : eq.getRequiredDocuments()) reqDocMap.putIfAbsent(ci.getId(), ci.getKey());
        }
        if (r.getExtraRequiredDocuments() != null) {
            for (ConfigItem ci : r.getExtraRequiredDocuments()) reqDocMap.putIfAbsent(ci.getId(), ci.getKey());
        }
        base.setRequiredDocuments(reqDocMap.entrySet().stream()
                .map(en -> RequiredDocDto.builder()
                        .id(en.getKey())
                        .name(en.getValue())
                        .checked(checkedIds.contains(en.getKey()))
                        .build())
                .toList());
        base.setDispatchedAt(plan.getDispatchedAt());
        base.setDeliveredAt(plan.getDeliveredAt());
        base.setDeliveryNotes(plan.getDeliveryNotes());

        if (plan.getWinnerItem() != null) {
            var w = plan.getWinnerItem();
            base.setWinnerItemId(w.getId());
            base.setWinnerPartyType(w.getPartyType() != null ? w.getPartyType().name() : null);
            base.setWinnerPartyName(w.getContractor() != null
                    ? w.getContractor().getCompanyName()
                    : (w.getInvestor() != null ? w.getInvestor().getCompanyName() : null));
            if (w.getEquipment() != null) {
                base.setWinnerEquipmentName(w.getEquipment().getName());
                base.setWinnerEquipmentCode(w.getEquipment().getEquipmentCode());
            }
        }
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

        // Çoxlu texnika xətləri
        base.setItems(plan.getItems().stream()
                .filter(it -> !it.isDeleted())
                .map(it -> {
                    String partyName = it.getContractor() != null ? it.getContractor().getCompanyName()
                            : it.getInvestor() != null ? it.getInvestor().getCompanyName()
                            : "Şirkət";
                    // Bu xəttin yoxlama checklist-i (texnika məcburi ∪ LM əlavə)
                    Set<Long> itChecked = it.getCheckedDocumentItemIds() != null ? it.getCheckedDocumentItemIds() : Set.of();
                    Map<Long, String> itReq = new LinkedHashMap<>();
                    if (it.getEquipment() != null && it.getEquipment().getRequiredDocuments() != null) {
                        for (ConfigItem ci : it.getEquipment().getRequiredDocuments()) itReq.putIfAbsent(ci.getId(), ci.getKey());
                    }
                    if (r.getExtraRequiredDocuments() != null) {
                        for (ConfigItem ci : r.getExtraRequiredDocuments()) itReq.putIfAbsent(ci.getId(), ci.getKey());
                    }
                    List<RequiredDocDto> itReqDocs = itReq.entrySet().stream()
                            .map(en -> RequiredDocDto.builder().id(en.getKey()).name(en.getValue())
                                    .checked(itChecked.contains(en.getKey())).build())
                            .toList();
                    // Bu xəttin təhvil-təslim aktı
                    var actDoc = plan.getDocuments().stream()
                            .filter(d -> !d.isDeleted() && "HANDOVER_ACT".equals(d.getDocumentType())
                                    && d.getPlanItem() != null && d.getPlanItem().getId().equals(it.getId()))
                            .findFirst().orElse(null);
                    return PlanItemDto.builder()
                            .id(it.getId())
                            .shortlistItemId(it.getShortlistItem() != null ? it.getShortlistItem().getId() : null)
                            .partyType(it.getPartyType() != null ? it.getPartyType().name() : null)
                            .partyName(partyName)
                            .equipmentId(it.getEquipment() != null ? it.getEquipment().getId() : null)
                            .equipmentName(it.getEquipment() != null ? it.getEquipment().getName() : null)
                            .equipmentCode(it.getEquipment() != null ? it.getEquipment().getEquipmentCode() : null)
                            .equipmentPrice(it.getEquipmentPrice())
                            .customerEquipmentPrice(it.getCustomerEquipmentPrice())
                            .transportationPrice(it.getTransportationPrice())
                            .agreedEquipmentPrice(it.getAgreedEquipmentPrice())
                            .agreedTransportPrice(it.getAgreedTransportPrice())
                            .agreedTotalPrice(it.getAgreedTotalPrice())
                            .agreementNote(it.getAgreementNote())
                            .dayCount(it.getDayCount())
                            .startDate(it.getStartDate())
                            .endDate(it.getEndDate())
                            .operatorId(it.getOperator() != null ? it.getOperator().getId() : null)
                            .operatorName(it.getOperator() != null
                                    ? it.getOperator().getFirstName() + " " + it.getOperator().getLastName() : null)
                            .equipmentDocsVerified(it.isEquipmentDocsVerified())
                            .equipmentDocsCheckedAt(it.getEquipmentDocsCheckedAt())
                            .dispatchedAt(it.getDispatchedAt())
                            .deliveredAt(it.getDeliveredAt())
                            .deliveryNotes(it.getDeliveryNotes())
                            .checkedDocumentItemIds(it.getCheckedDocumentItemIds())
                            .requiredDocuments(itReqDocs)
                            .actDocumentId(actDoc != null ? actDoc.getId() : null)
                            .actFileName(actDoc != null ? actDoc.getDocumentName() : null)
                            .equipmentDocuments(it.getEquipment() != null && it.getEquipment().getDocuments() != null
                                    ? it.getEquipment().getDocuments().stream()
                                        .map(ed -> EquipmentDocDto.builder()
                                                .id(ed.getId())
                                                .name(ed.getDocumentName())
                                                .type(ed.getDocumentType())
                                                .build())
                                        .toList()
                                    : List.of())
                            .build();
                })
                .toList());

        return base;
    }
}
