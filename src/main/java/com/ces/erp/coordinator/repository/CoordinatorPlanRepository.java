package com.ces.erp.coordinator.repository;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CoordinatorPlanRepository extends JpaRepository<CoordinatorPlan, Long> {

    @Query("SELECT p FROM CoordinatorPlan p LEFT JOIN FETCH p.documents LEFT JOIN FETCH p.selectedEquipment WHERE p.request.id = :requestId AND p.deleted = false")
    Optional<CoordinatorPlan> findByRequestId(Long requestId);

    boolean existsByRequestIdAndDeletedFalse(Long requestId);
}
