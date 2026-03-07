package com.ces.erp.project.repository;

import com.ces.erp.project.entity.ProjectRevenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRevenueRepository extends JpaRepository<ProjectRevenue, Long> {
    List<ProjectRevenue> findAllByProjectIdAndDeletedFalse(Long projectId);
    Optional<ProjectRevenue> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);
}
