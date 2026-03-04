package com.ces.erp.role.repository;

import com.ces.erp.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByDeletedFalse();

    List<Role> findAllByDepartmentIdAndDeletedFalse(Long departmentId);

    Optional<Role> findByIdAndDeletedFalse(Long id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions p LEFT JOIN FETCH p.module WHERE r.id = :id AND r.deleted = false")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);
}
