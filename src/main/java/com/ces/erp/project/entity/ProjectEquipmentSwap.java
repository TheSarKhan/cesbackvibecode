package com.ces.erp.project.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.garage.entity.Equipment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "project_equipment_swaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEquipmentSwap extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_equipment_id", nullable = false)
    private Equipment oldEquipment;

    private Double oldEquipmentFinalCounter;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String oldEquipmentNextStatus = "IN_REPAIR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_equipment_id", nullable = false)
    private Equipment newEquipment;

    private Double newEquipmentInitialCounter;

    @Column(nullable = false)
    private LocalDate swapDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String swapReason;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
