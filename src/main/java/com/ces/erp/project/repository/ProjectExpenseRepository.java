package com.ces.erp.project.repository;

import com.ces.erp.project.entity.ProjectExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, Long> {
    List<ProjectExpense> findAllByProjectIdAndDeletedFalse(Long projectId);
    Optional<ProjectExpense> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);
}
