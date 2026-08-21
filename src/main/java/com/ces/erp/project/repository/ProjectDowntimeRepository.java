package com.ces.erp.project.repository;

import com.ces.erp.project.entity.ProjectDowntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDowntimeRepository extends JpaRepository<ProjectDowntime, Long> {
    List<ProjectDowntime> findByProjectIdAndDeletedFalseOrderByStartDateDesc(Long projectId);
    List<ProjectDowntime> findByProjectIdAndStatusAndDeletedFalse(Long projectId, String status);
}
