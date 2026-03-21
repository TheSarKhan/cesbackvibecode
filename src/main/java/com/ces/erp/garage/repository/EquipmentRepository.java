package com.ces.erp.garage.repository;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.garage.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.responsibleUser LEFT JOIN FETCH e.ownerContractor WHERE e.deleted = false")
    List<Equipment> findAllByDeletedFalse();

    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.responsibleUser LEFT JOIN FETCH e.ownerContractor WHERE e.id = :id AND e.deleted = false")
    Optional<Equipment> findByIdWithDetails(@Param("id") Long id);

    boolean existsByEquipmentCodeAndDeletedFalse(String equipmentCode);

    boolean existsByEquipmentCodeAndIdNotAndDeletedFalse(String equipmentCode, Long id);

    boolean existsBySerialNumberAndDeletedFalse(String serialNumber);

    boolean existsBySerialNumberAndIdNotAndDeletedFalse(String serialNumber, Long id);

    List<Equipment> findAllByStatusAndDeletedFalse(EquipmentStatus status);

    List<Equipment> findAllByDeletedTrue();
}
