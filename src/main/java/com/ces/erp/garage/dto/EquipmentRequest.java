package com.ces.erp.garage.dto;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OwnershipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class EquipmentRequest {

    @NotBlank(message = "Texnika kodu boş ola bilməz")
    private String equipmentCode;

    @NotBlank(message = "Ad boş ola bilməz")
    private String name;

    @NotBlank(message = "Növ boş ola bilməz")
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

    @NotNull(message = "Mülkiyyət növü boş ola bilməz")
    private OwnershipType ownershipType;

    // CONTRACTOR üçün
    private Long ownerContractorId;

    // INVESTOR üçün
    private String ownerInvestorName;
    private String ownerInvestorVoen;
    private String ownerInvestorPhone;

    private LocalDate lastInspectionDate;
    private LocalDate nextInspectionDate;
    private String technicalReadinessStatus;

    @NotNull(message = "Status boş ola bilməz")
    private EquipmentStatus status;

    private String repairStatus;
    private String notes;

    private List<Long> safetyEquipmentIds;
}
