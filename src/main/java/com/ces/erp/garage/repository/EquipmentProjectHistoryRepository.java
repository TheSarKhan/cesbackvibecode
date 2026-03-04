package com.ces.erp.garage.repository;

import com.ces.erp.garage.entity.EquipmentProjectHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentProjectHistoryRepository extends JpaRepository<EquipmentProjectHistory, Long> {

    List<EquipmentProjectHistory> findAllByEquipmentIdOrderByStartDateDesc(Long equipmentId);
}
