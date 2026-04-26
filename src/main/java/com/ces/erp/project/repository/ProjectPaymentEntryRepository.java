package com.ces.erp.project.repository;

import com.ces.erp.project.entity.ProjectPaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectPaymentEntryRepository extends JpaRepository<ProjectPaymentEntry, Long> {
    List<ProjectPaymentEntry> findAllByProjectIdAndDeletedFalseOrderByPaymentDateAsc(Long projectId);
    Optional<ProjectPaymentEntry> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);
}
