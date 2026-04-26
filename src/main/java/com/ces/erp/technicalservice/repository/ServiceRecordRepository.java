package com.ces.erp.technicalservice.repository;

import com.ces.erp.technicalservice.entity.ServiceRecord;
import com.ces.erp.technicalservice.entity.ServiceRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    @Query("SELECT s FROM ServiceRecord s WHERE s.deleted = false ORDER BY s.serviceDate DESC")
    List<ServiceRecord> findAllActive();

    @Query("SELECT s FROM ServiceRecord s WHERE s.deleted = false AND s.recordType = :recordType ORDER BY s.serviceDate DESC")
    List<ServiceRecord> findAllActiveByType(@Param("recordType") ServiceRecordType recordType);

    @Query("SELECT s FROM ServiceRecord s WHERE s.equipment.id = :equipmentId AND s.deleted = false ORDER BY s.serviceDate DESC")
    List<ServiceRecord> findAllByEquipmentId(@Param("equipmentId") Long equipmentId);

    @Query("SELECT COUNT(s) > 0 FROM ServiceRecord s WHERE s.equipment.id = :equipmentId AND s.deleted = false AND s.completed = false AND s.recordType = :recordType")
    boolean existsOpenByEquipmentAndType(@Param("equipmentId") Long equipmentId, @Param("recordType") ServiceRecordType recordType);
}
