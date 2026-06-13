package com.ces.erp.projectmanager.dto;

import com.ces.erp.coordinator.dto.CoordinatorPlanResponse;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.entity.TechRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Layihə Menecerinin gördüyü sorğu məlumatı.
 * Siyahı və detal — eyni response istifadə olunur.
 */
@Data
@Builder
public class PmRequestResponse {

    // Sorğu əsas məlumatları
    private Long requestId;
    private String requestCode;
    private RequestStatus status;
    private LocalDateTime createdAt;

    // Müştəri
    private Long customerId;
    private String companyName;
    private String contactPerson;
    private String contactPhone;

    // Layihə
    private String projectName;
    private String region;
    private LocalDate requestDate;
    private ProjectType projectType;
    private Integer dayCount;
    private boolean transportationRequired;

    private String notes;
    private BigDecimal agreedEquipmentPrice;
    private BigDecimal agreedTransportPrice;
    private BigDecimal agreedTotalPrice;

    // PM-in əlavə etdiyi sifarişçi ofis kontaktı (LM addımı 1.3)
    private String customerOfficeContact;
    private String customerOfficePhone;

    // Sorğunun texniki parametrləri (key-value)
    private List<ParamDto> params;

    // PM-in yaratdığı shortlist (detal görünüşdə)
    private Long shortlistId;
    private String shortlistNotes;
    private List<ShortlistItemDto> shortlistItems;

    // Koordinatorun göndərdiyi təklif (status >= COORDINATOR_PROPOSED olduqda)
    private CoordinatorPlanResponse coordinatorOffer;

    // PM tərəfindən yüklənmiş sənədlər (müqavilə + qiymət protokolu)
    private boolean contractUploaded;
    private boolean priceProtocolUploaded;
    private List<DocumentDto> documents;

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
        private String docType;           // CONTRACT | PRICE_PROTOCOL
        private String fileName;
        private String uploadedByName;
        private java.time.LocalDateTime uploadedAt;
    }

    public static PmRequestResponse fromList(TechRequest r) {
        return PmRequestResponse.builder()
                .requestId(r.getId())
                .requestCode(r.getRequestCode() != null ? r.getRequestCode() : "REQ-" + String.format("%04d", r.getId()))
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .customerId(r.getCustomer() != null ? r.getCustomer().getId() : null)
                .companyName(r.getCompanyName())
                .contactPerson(r.getContactPerson())
                .contactPhone(r.getContactPhone())
                .projectName(r.getProjectName())
                .region(r.getRegion())
                .requestDate(r.getRequestDate())
                .projectType(r.getProjectType())
                .dayCount(r.getDayCount())
                .transportationRequired(r.isTransportationRequired())
                .notes(r.getNotes())
                .agreedEquipmentPrice(r.getAgreedEquipmentPrice())
                .agreedTransportPrice(r.getAgreedTransportPrice())
                .agreedTotalPrice(r.getAgreedTotalPrice())
                .customerOfficeContact(r.getCustomerOfficeContact())
                .customerOfficePhone(r.getCustomerOfficePhone())
                .build();
    }
}
