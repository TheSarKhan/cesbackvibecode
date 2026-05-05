package com.ces.erp.coordinator.repository;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CoordinatorPlanRepository extends JpaRepository<CoordinatorPlan, Long> {

    @Query("SELECT p FROM CoordinatorPlan p LEFT JOIN FETCH p.documents LEFT JOIN FETCH p.selectedEquipment WHERE p.request.id = :requestId AND p.deleted = false")
    Optional<CoordinatorPlan> findByRequestId(Long requestId);

    boolean existsByRequestIdAndDeletedFalse(Long requestId);

    @Query("""
            SELECT cp FROM CoordinatorPlan cp
            LEFT JOIN FETCH cp.selectedEquipment
            WHERE cp.operator.id = :operatorId
              AND cp.deleted = false
            ORDER BY cp.createdAt DESC
            """)
    List<CoordinatorPlan> findAllByOperatorId(@Param("operatorId") Long operatorId);

    @Query("""
            SELECT cp FROM CoordinatorPlan cp
            LEFT JOIN FETCH cp.selectedEquipment eq
            WHERE eq.ownerContractor.id = :contractorId
              AND cp.deleted = false
            ORDER BY cp.createdAt DESC
            """)
    List<CoordinatorPlan> findAllByEquipmentContractorId(@Param("contractorId") Long contractorId);

    @Query("""
            SELECT cp FROM CoordinatorPlan cp
            LEFT JOIN FETCH cp.selectedEquipment eq
            WHERE eq.ownerInvestorVoen = :voen
              AND cp.deleted = false
            ORDER BY cp.createdAt DESC
            """)
    List<CoordinatorPlan> findAllByEquipmentInvestorVoen(@Param("voen") String voen);

    /**
     * Operatorun hal-hazırda başqa aktiv layihəyə (PENDING/ACTIVE) təyin olub-olmadığını yoxlayır.
     * excludeRequestId — cari planı kənar saxlamaq üçün.
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM CoordinatorPlan p
            WHERE p.operator.id = :operatorId
              AND p.deleted = false
              AND p.request.id <> :excludeRequestId
              AND EXISTS (
                  SELECT proj FROM Project proj
                  WHERE proj.request = p.request
                    AND proj.deleted = false
                    AND proj.status IN :activeStatuses
              )
            """)
    boolean isOperatorBusyInOtherProject(@Param("operatorId") Long operatorId,
                                         @Param("excludeRequestId") Long excludeRequestId,
                                         @Param("activeStatuses") List<ProjectStatus> activeStatuses);
}
