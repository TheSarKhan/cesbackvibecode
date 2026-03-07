package com.ces.erp.garage.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String equipmentCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(unique = true, length = 100)
    private String serialNumber;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    private Integer manufactureYear;

    private LocalDate purchaseDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    @Column(length = 50)
    private String plateNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightTon;

    @Column(precision = 15, scale = 2)
    private BigDecimal currentMarketValue;

    @Column(precision = 5, scale = 2)
    private BigDecimal depreciationRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal hourKmCounter;

    @Column(precision = 12, scale = 2)
    private BigDecimal motoHours;

    private String storageLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnershipType ownershipType;

    // Podratçı texnikası
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_contractor_id")
    private Contractor ownerContractor;

    // İnvestor texnikası
    @Column(length = 255)
    private String ownerInvestorName;

    @Column(length = 20)
    private String ownerInvestorVoen;

    @Column(length = 50)
    private String ownerInvestorPhone;

    private LocalDate lastInspectionDate;

    private LocalDate nextInspectionDate;

    @Column(length = 100)
    private String technicalReadinessStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    private String repairStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EquipmentInspection> inspections = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EquipmentDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EquipmentImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EquipmentProjectHistory> projectHistory = new ArrayList<>();
}
