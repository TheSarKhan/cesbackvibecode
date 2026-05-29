package com.ces.erp.permission.repository;

import com.ces.erp.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderByModuleCodeAscActionAsc();

    /** Xəyali icazələrin rol-link sətirlərini silir (FK səbəbi ilə permission silinməzdən əvvəl). */
    @Modifying
    @Query(value = "DELETE FROM role_granted_permission WHERE permission_id IN (:ids)", nativeQuery = true)
    void deleteGrantLinksByPermissionIds(@Param("ids") Collection<Long> ids);
}
