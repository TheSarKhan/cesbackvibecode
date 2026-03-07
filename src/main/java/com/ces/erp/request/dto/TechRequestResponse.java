package com.ces.erp.request.dto;

import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.entity.TechParam;
import com.ces.erp.request.entity.TechRequest;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TechRequestResponse {

    private Long id;
    private String requestCode;
    private RequestStatus status;

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

    // Daşınma
    private boolean transportationRequired;

    // Texniki parametrlər
    private List<ParamDto> params;

    // Seçilmiş texnika
    private Long selectedEquipmentId;
    private String selectedEquipmentName;
    private String selectedEquipmentCode;

    // Meta
    private String createdByName;
    private LocalDateTime createdAt;
    private String notes;

    @Data
    @Builder
    public static class ParamDto {
        private String paramKey;
        private String paramValue;
    }

    public static TechRequestResponse from(TechRequest r) {
        List<ParamDto> params = r.getParams().stream()
                .map(p -> ParamDto.builder().paramKey(p.getParamKey()).paramValue(p.getParamValue()).build())
                .toList();

        return TechRequestResponse.builder()
                .id(r.getId())
                .requestCode("REQ-" + String.format("%04d", r.getId()))
                .status(r.getStatus())
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
                .params(params)
                .selectedEquipmentId(r.getSelectedEquipment() != null ? r.getSelectedEquipment().getId() : null)
                .selectedEquipmentName(r.getSelectedEquipment() != null ? r.getSelectedEquipment().getName() : null)
                .selectedEquipmentCode(r.getSelectedEquipment() != null ? r.getSelectedEquipment().getEquipmentCode() : null)
                .createdByName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null)
                .createdAt(r.getCreatedAt())
                .notes(r.getNotes())
                .build();
    }
}
