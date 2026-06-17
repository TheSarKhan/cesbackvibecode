package com.ces.erp.coordinator.repository;

import com.ces.erp.coordinator.entity.CoordinatorPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoordinatorPlanItemRepository extends JpaRepository<CoordinatorPlanItem, Long> {

    List<CoordinatorPlanItem> findAllByPlanIdAndDeletedFalse(Long planId);

    Optional<CoordinatorPlanItem> findByIdAndDeletedFalse(Long id);
}
