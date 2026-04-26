package com.ces.erp.technicalservice.dto;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.technicalservice.entity.ServiceRecordType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ServiceRecordRequest {
    private Long equipmentId;
    private Long contractorId;
    private String serviceType;
    private String description;
    private BigDecimal cost;
    private LocalDate serviceDate;
    private LocalDate nextServiceDate;
    private Integer odometer;
    private String notes;
    private EquipmentStatus statusAfter;
    private ServiceRecordType recordType;
    private java.util.List<ServiceChecklistItemDto> checklistItems;
}
