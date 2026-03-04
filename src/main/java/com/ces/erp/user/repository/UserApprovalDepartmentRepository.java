package com.ces.erp.user.repository;

import com.ces.erp.user.entity.UserApprovalDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserApprovalDepartmentRepository extends JpaRepository<UserApprovalDepartment, Long> {

    List<UserApprovalDepartment> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
