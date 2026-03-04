package com.ces.erp.garage.repository;

import com.ces.erp.garage.entity.EquipmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentDocumentRepository extends JpaRepository<EquipmentDocument, Long> {

    List<EquipmentDocument> findAllByEquipmentId(Long equipmentId);

    Optional<EquipmentDocument> findByIdAndEquipmentId(Long id, Long equipmentId);
}
