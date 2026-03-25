package com.ces.erp.project.repository;

import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.request WHERE p.deleted = false ORDER BY p.createdAt DESC")
    List<Project> findAllWithFinances();

    @Query(value = "SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.request r WHERE p.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(COALESCE(p.projectCode, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.projectName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (CAST(:status AS string) IS NULL OR p.status = :status)",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Project p JOIN p.request r WHERE p.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(COALESCE(p.projectCode, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.projectName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (CAST(:status AS string) IS NULL OR p.status = :status)")
    Page<Project> findAllFiltered(@Param("search") String search,
                                  @Param("status") ProjectStatus status,
                                  Pageable pageable);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.request WHERE p.id = :id AND p.deleted = false")
    Optional<Project> findByIdWithFinances(Long id);

    Optional<Project> findByRequestIdAndDeletedFalse(Long requestId);

    boolean existsByRequestIdAndDeletedFalse(Long requestId);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(project_code FROM 5) AS INTEGER)), 0) FROM projects WHERE project_code LIKE 'PRJ-%'", nativeQuery = true)
    int findMaxProjectCodeNumber();

    List<Project> findAllByDeletedTrue();

    long countByStatusAndDeletedFalse(ProjectStatus status);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.deleted = true")
    long countByDeletedTrue();
}
