package com.ces.erp.coordinator.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.request.entity.TechRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coordinator_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinatorPlan extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private TechRequest request;

    // Koordinatorun seçdiyi texnika
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment selectedEquipment;

    private String operatorName;

    @Column(precision = 12, scale = 2)
    private BigDecimal equipmentPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal contractorPayment;

    @Column(precision = 12, scale = 2)
    private BigDecimal transportationPrice;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean hasFlashingLights = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean hasFireExtinguisher = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean hasFirstAid = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CoordinatorDocument> documents = new ArrayList<>();
}
