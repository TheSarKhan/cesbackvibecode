package com.ces.erp.technicalservice.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.garage.entity.Equipment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    @Column(nullable = false)
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate serviceDate;

    private LocalDate nextServiceDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    private Integer odometer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus statusBefore;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus statusAfter;

    @Builder.Default
    private boolean completed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private ServiceRecordType recordType;

    @OneToMany(mappedBy = "serviceRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServiceChecklistItem> checklistItems = new ArrayList<>();
}
