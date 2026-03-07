package com.ces.erp.garage.dto;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.garage.entity.Equipment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EquipmentResponse {

    private Long id;
    private String equipmentCode;
    private String name;
    private String type;
    private String serialNumber;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private String plateNumber;
    private BigDecimal weightTon;
    private BigDecimal currentMarketValue;
    private BigDecimal depreciationRate;
    private BigDecimal hourKmCounter;
    private BigDecimal motoHours;
    private String storageLocation;
    private Long responsibleUserId;
    private String responsibleUserName;
    private OwnershipType ownershipType;

    // Company / Investor / Contractor ownership
    private Long ownerContractorId;
    private String ownerContractorName;
    private String ownerContractorVoen;
    private String ownerContractorPhone;
    private String ownerContractorContact;

    private String ownerInvestorName;
    private String ownerInvestorVoen;
    private String ownerInvestorPhone;

    // Inspection dates
    private LocalDate lastInspectionDate;
    private LocalDate nextInspectionDate;

    // Statuses
    private String technicalReadinessStatus;
    private EquipmentStatus status;
    private String repairStatus;

    private String notes;

    private List<InspectionResponse> inspections;
    private List<DocumentResponse> documents;
    private List<ImageResponse> images;
    private LocalDateTime createdAt;

    public static EquipmentResponse from(Equipment e) {
        return EquipmentResponse.builder()
                .id(e.getId())
                .equipmentCode(e.getEquipmentCode())
                .name(e.getName())
                .type(e.getType())
                .serialNumber(e.getSerialNumber())
                .brand(e.getBrand())
                .model(e.getModel())
                .manufactureYear(e.getManufactureYear())
                .purchaseDate(e.getPurchaseDate())
                .purchasePrice(e.getPurchasePrice())
                .plateNumber(e.getPlateNumber())
                .weightTon(e.getWeightTon())
                .currentMarketValue(e.getCurrentMarketValue())
                .depreciationRate(e.getDepreciationRate())
                .hourKmCounter(e.getHourKmCounter())
                .motoHours(e.getMotoHours())
                .storageLocation(e.getStorageLocation())
                .responsibleUserId(e.getResponsibleUser() != null ? e.getResponsibleUser().getId() : null)
                .responsibleUserName(e.getResponsibleUser() != null ? e.getResponsibleUser().getFullName() : null)
                .ownershipType(e.getOwnershipType())
                .ownerContractorId(e.getOwnerContractor() != null ? e.getOwnerContractor().getId() : null)
                .ownerContractorName(e.getOwnerContractor() != null ? e.getOwnerContractor().getCompanyName() : null)
                .ownerContractorVoen(e.getOwnerContractor() != null ? e.getOwnerContractor().getVoen() : null)
                .ownerContractorPhone(e.getOwnerContractor() != null ? e.getOwnerContractor().getPhone() : null)
                .ownerContractorContact(e.getOwnerContractor() != null ? e.getOwnerContractor().getContactPerson() : null)
                .ownerInvestorName(e.getOwnerInvestorName())
                .ownerInvestorVoen(e.getOwnerInvestorVoen())
                .ownerInvestorPhone(e.getOwnerInvestorPhone())
                .lastInspectionDate(e.getLastInspectionDate())
                .nextInspectionDate(e.getNextInspectionDate())
                .technicalReadinessStatus(e.getTechnicalReadinessStatus())
                .status(e.getStatus())
                .repairStatus(e.getRepairStatus())
                .notes(e.getNotes())
                .inspections(e.getInspections().stream().map(InspectionResponse::from).toList())
                .documents(e.getDocuments().stream().map(DocumentResponse::from).toList())
                .images(e.getImages().stream().map(ImageResponse::from).toList())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
