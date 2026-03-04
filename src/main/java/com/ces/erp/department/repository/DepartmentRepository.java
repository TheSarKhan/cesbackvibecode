package com.ces.erp.department.repository;

import com.ces.erp.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByDeletedFalse();

    Optional<Department> findByIdAndDeletedFalse(Long id);

    boolean existsByNameAndDeletedFalse(String name);
}
