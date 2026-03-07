package com.ces.erp.garage.repository;

import com.ces.erp.garage.entity.EquipmentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipmentImageRepository extends JpaRepository<EquipmentImage, Long> {
    Optional<EquipmentImage> findByIdAndEquipmentId(Long id, Long equipmentId);
}
