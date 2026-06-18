package com.ces.erp.garage.repository;

import com.ces.erp.garage.entity.EquipmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentDocumentRepository extends JpaRepository<EquipmentDocument, Long> {

    List<EquipmentDocument> findAllByEquipmentId(Long equipmentId);

    Optional<EquipmentDocument> findByIdAndEquipmentId(Long id, Long equipmentId);

    List<EquipmentDocument> findAllByEquipmentIdAndDocumentType(Long equipmentId, String documentType);

    // ─── Sənəd mərkəzi: sahibə görə texnika sənədləri ────────────────────────

    /** Podratçıya məxsus texnikaların qaraj sənədləri. */
    @Query("""
            SELECT d FROM EquipmentDocument d
            WHERE d.equipment.ownerContractor.id = :contractorId
            ORDER BY d.createdAt DESC
            """)
    List<EquipmentDocument> findByOwnerContractor(@Param("contractorId") Long contractorId);

    /** İnvestora (VÖEN üzrə) məxsus texnikaların qaraj sənədləri. */
    @Query("""
            SELECT d FROM EquipmentDocument d
            WHERE d.equipment.ownerInvestorVoen = :voen
            ORDER BY d.createdAt DESC
            """)
    List<EquipmentDocument> findByOwnerInvestorVoen(@Param("voen") String voen);
}
