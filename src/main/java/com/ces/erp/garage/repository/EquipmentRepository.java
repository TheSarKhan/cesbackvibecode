package com.ces.erp.garage.repository;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.garage.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByIdAndDeletedFalse(Long id);

    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.responsibleUser LEFT JOIN FETCH e.ownerContractor WHERE e.deleted = false")
    List<Equipment> findAllByDeletedFalse();

    @EntityGraph(attributePaths = {"responsibleUser", "ownerContractor"})
    @Query("SELECT e FROM Equipment e WHERE e.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(e.brand, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(e.equipmentCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(e.serialNumber, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(e.storageLocation, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (CAST(:status AS string) IS NULL OR e.status = :status)" +
            " AND (CAST(:ownershipType AS string) IS NULL OR e.ownershipType = :ownershipType)" +
            " AND (CAST(:type AS string) IS NULL OR e.type = :type)" +
            " AND (CAST(:brand AS string) IS NULL OR e.brand = :brand)" +
            " AND (CAST(:location AS string) IS NULL OR e.storageLocation = :location)" +
            " AND (:priceMin IS NULL OR e.purchasePrice >= :priceMin)" +
            " AND (:priceMax IS NULL OR e.purchasePrice <= :priceMax)" +
            " AND (:yearMin IS NULL OR e.manufactureYear >= :yearMin)" +
            " AND (:yearMax IS NULL OR e.manufactureYear <= :yearMax)" +
            " AND (:motoMin IS NULL OR e.motoHours >= :motoMin)" +
            " AND (:motoMax IS NULL OR e.motoHours <= :motoMax)")
    Page<Equipment> findAllFiltered(
            @Param("search") String search,
            @Param("status") EquipmentStatus status,
            @Param("ownershipType") OwnershipType ownershipType,
            @Param("type") String type,
            @Param("brand") String brand,
            @Param("location") String location,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            @Param("yearMin") Integer yearMin,
            @Param("yearMax") Integer yearMax,
            @Param("motoMin") BigDecimal motoMin,
            @Param("motoMax") BigDecimal motoMax,
            Pageable pageable);

    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.responsibleUser LEFT JOIN FETCH e.ownerContractor WHERE e.id = :id AND e.deleted = false")
    Optional<Equipment> findByIdWithDetails(@Param("id") Long id);

    boolean existsByEquipmentCodeAndDeletedFalse(String equipmentCode);

    boolean existsByEquipmentCodeAndIdNotAndDeletedFalse(String equipmentCode, Long id);

    boolean existsBySerialNumberAndDeletedFalse(String serialNumber);

    boolean existsBySerialNumberAndIdNotAndDeletedFalse(String serialNumber, Long id);

    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.responsibleUser LEFT JOIN FETCH e.ownerContractor WHERE e.ownerContractor.id = :contractorId AND e.deleted = false")
    List<Equipment> findAllByOwnerContractorIdAndDeletedFalse(@Param("contractorId") Long contractorId);

    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.responsibleUser LEFT JOIN FETCH e.ownerContractor WHERE ((:voen IS NOT NULL AND e.ownerInvestorVoen = :voen) OR (:name IS NOT NULL AND e.ownerInvestorName = :name)) AND e.deleted = false")
    List<Equipment> findAllByInvestor(@Param("voen") String voen, @Param("name") String name);

    List<Equipment> findAllByStatusAndDeletedFalse(EquipmentStatus status);

    List<Equipment> findAllByDeletedTrue();

    long countByStatusAndDeletedFalse(EquipmentStatus status);

    long countByDeletedTrue();
}
