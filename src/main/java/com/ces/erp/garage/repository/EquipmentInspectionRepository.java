package com.ces.erp.garage.repository;

import com.ces.erp.garage.entity.EquipmentInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentInspectionRepository extends JpaRepository<EquipmentInspection, Long> {

    List<EquipmentInspection> findAllByEquipmentIdOrderByInspectionDateDesc(Long equipmentId);

    Optional<EquipmentInspection> findByIdAndEquipmentId(Long id, Long equipmentId);
}
