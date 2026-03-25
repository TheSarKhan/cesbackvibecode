package com.ces.erp.role.repository;

import com.ces.erp.role.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByDeletedFalse();

    @Query("SELECT r FROM Role r WHERE r.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (:departmentId IS NULL OR r.department.id = :departmentId)")
    Page<Role> findAllFiltered(@Param("search") String search,
                               @Param("departmentId") Long departmentId,
                               Pageable pageable);

    List<Role> findAllByDepartmentIdAndDeletedFalse(Long departmentId);

    Optional<Role> findByIdAndDeletedFalse(Long id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions p LEFT JOIN FETCH p.module WHERE r.id = :id AND r.deleted = false")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    List<Role> findAllByDeletedTrue();
}
