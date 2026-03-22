package com.ces.erp.garage.repository;

import com.ces.erp.garage.entity.EquipmentStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentStatusLogRepository extends JpaRepository<EquipmentStatusLog, Long> {

    List<EquipmentStatusLog> findAllByEquipmentIdOrderByChangedAtDesc(Long equipmentId);
}
