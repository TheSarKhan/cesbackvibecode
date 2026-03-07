package com.ces.erp.project.repository;

import com.ces.erp.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.request WHERE p.deleted = false ORDER BY p.createdAt DESC")
    List<Project> findAllWithFinances();

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.request WHERE p.id = :id AND p.deleted = false")
    Optional<Project> findByIdWithFinances(Long id);

    Optional<Project> findByRequestIdAndDeletedFalse(Long requestId);

    boolean existsByRequestIdAndDeletedFalse(Long requestId);
}
