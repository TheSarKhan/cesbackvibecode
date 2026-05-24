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
    private BigDecimal agreedTotalPrice;

    // Sorğunun texniki parametrləri (key-value)
    private List<ParamDto> params;

    // PM-in yaratdığı shortlist (detal görünüşdə)
    private Long shortlistId;
    private String shortlistNotes;
    private List<ShortlistItemDto> shortlistItems;

    // Koordinatorun göndərdiyi təklif (status >= COORDINATOR_PROPOSED olduqda)
    private CoordinatorPlanResponse coordinatorOffer;

    @Data
    @Builder
    public static class ParamDto {
        private String paramKey;
        private String paramValue;
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
                .agreedTotalPrice(r.getAgreedTotalPrice())
                .build();
    }
}
