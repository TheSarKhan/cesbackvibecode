package com.ces.erp.coordinator.repository;

import com.ces.erp.coordinator.entity.CoordinatorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoordinatorDocumentRepository extends JpaRepository<CoordinatorDocument, Long> {

    Optional<CoordinatorDocument> findByIdAndPlanIdAndDeletedFalse(Long id, Long planId);

    List<CoordinatorDocument> findAllByPlanIdAndDocumentTypeAndDeletedFalse(Long planId, String documentType);

    // Texnika xəttinə bağlı sənədlər (akt və s.)
    List<CoordinatorDocument> findAllByPlanItemIdAndDeletedFalse(Long planItemId);

    List<CoordinatorDocument> findAllByPlanItemIdAndDocumentTypeAndDeletedFalse(Long planItemId, String documentType);

    // ─── Sənəd mərkəzi aqreqasiyası (tipə görə, məs. HANDOVER_ACT) ────────────

    /** Müştərinin bütün layihələrindəki sənədlər (verilmiş tipdə). */
    @Query("""
            SELECT d FROM CoordinatorDocument d
            WHERE d.deleted = false AND d.documentType = :type
              AND d.plan.request.customer.id = :customerId
            ORDER BY d.createdAt DESC
            """)
    List<CoordinatorDocument> findByCustomerAndType(@Param("customerId") Long customerId,
                                                    @Param("type") String type);

    /** Podratçının iştirak etdiyi xətlərdəki sənədlər (verilmiş tipdə). */
    @Query("""
            SELECT d FROM CoordinatorDocument d
            WHERE d.deleted = false AND d.documentType = :type
              AND d.planItem.contractor.id = :contractorId
            ORDER BY d.createdAt DESC
            """)
    List<CoordinatorDocument> findByContractorAndType(@Param("contractorId") Long contractorId,
                                                      @Param("type") String type);

    /** İnvestorun iştirak etdiyi xətlərdəki sənədlər (verilmiş tipdə). */
    @Query("""
            SELECT d FROM CoordinatorDocument d
            WHERE d.deleted = false AND d.documentType = :type
              AND d.planItem.investor.id = :investorId
            ORDER BY d.createdAt DESC
            """)
    List<CoordinatorDocument> findByInvestorAndType(@Param("investorId") Long investorId,
                                                    @Param("type") String type);
}
