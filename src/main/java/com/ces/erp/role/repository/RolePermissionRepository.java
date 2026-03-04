package com.ces.erp.role.repository;

import com.ces.erp.role.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findAllByRoleId(Long roleId);

    void deleteAllByRoleId(Long roleId);
}
