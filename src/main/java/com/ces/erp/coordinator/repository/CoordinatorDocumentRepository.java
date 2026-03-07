package com.ces.erp.coordinator.repository;

import com.ces.erp.coordinator.entity.CoordinatorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoordinatorDocumentRepository extends JpaRepository<CoordinatorDocument, Long> {

    Optional<CoordinatorDocument> findByIdAndPlanIdAndDeletedFalse(Long id, Long planId);
}
