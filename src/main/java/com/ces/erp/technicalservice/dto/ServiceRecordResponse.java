package com.ces.erp.technicalservice.dto;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.technicalservice.entity.ServiceRecord;
import com.ces.erp.technicalservice.entity.ServiceRecordType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ServiceRecordResponse {
    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String plateNumber;
    private Long contractorId;
    private String contractorName;
    private String serviceType;
    private String description;
    private BigDecimal cost;
    private LocalDate serviceDate;
    private LocalDate nextServiceDate;
    private Integer odometer;
    private String notes;
    private EquipmentStatus statusBefore;
    private EquipmentStatus statusAfter;
    private boolean completed;
    private ServiceRecordType recordType;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private List<ServiceChecklistItemDto> checklistItems;

    public static ServiceRecordResponse from(ServiceRecord s) {
        return ServiceRecordResponse.builder()
                .id(s.getId())
                .equipmentId(s.getEquipment().getId())
                .equipmentName(s.getEquipment().getName())
                .plateNumber(s.getEquipment().getPlateNumber())
                .contractorId(s.getContractor() != null ? s.getContractor().getId() : null)
                .contractorName(s.getContractor() != null ? s.getContractor().getCompanyName() : null)
                .serviceType(s.getServiceType())
                .description(s.getDescription())
                .cost(s.getCost())
                .serviceDate(s.getServiceDate())
                .nextServiceDate(s.getNextServiceDate())
                .odometer(s.getOdometer())
                .notes(s.getNotes())
                .statusBefore(s.getStatusBefore())
                .statusAfter(s.getStatusAfter())
                .completed(s.isCompleted())
                .recordType(s.getRecordType())
                .invoiceNumber(s.getInvoiceNumber())
                .invoiceDate(s.getInvoiceDate())
                .checklistItems(s.getChecklistItems() != null ? s.getChecklistItems().stream()
                        .map(item -> ServiceChecklistItemDto.builder()
                                .id(item.getId())
                                .itemName(item.getItemName())
                                .checked(item.isChecked())
                                .note(item.getNote())
                                .build())
                        .toList() : List.of())
                .build();
    }
}
