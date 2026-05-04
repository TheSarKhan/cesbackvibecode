package com.ces.erp.hr.repository;

import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.hr.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByDeletedFalseOrderByLastNameAsc();

    Optional<Employee> findByIdAndDeletedFalse(Long id);

    boolean existsByFinAndDeletedFalse(String fin);

    boolean existsByEmployeeCodeAndDeletedFalse(String code);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(EmployeeStatus status);

    List<Employee> findAllByStatusAndDeletedFalse(EmployeeStatus status);

    List<Employee> findAllByDepartmentIdAndDeletedFalse(Long departmentId);

    @Query("""
        SELECT e FROM Employee e
        WHERE e.deleted = false
          AND (:q IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(e.fin)       LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:status IS NULL OR e.status = :status)
          AND (:departmentId IS NULL OR e.department.id = :departmentId)
          AND (:positionId   IS NULL OR e.position.id   = :positionId)
        ORDER BY e.lastName ASC
        """)
    Page<Employee> searchPaged(@Param("q") String q,
                               @Param("status") EmployeeStatus status,
                               @Param("departmentId") Long departmentId,
                               @Param("positionId") Long positionId,
                               Pageable pageable);

    // Avtomatik kod generasiyası üçün
    @Query("SELECT MAX(e.employeeCode) FROM Employee e WHERE e.employeeCode LIKE :prefix%")
    Optional<String> findMaxEmployeeCodeWithPrefix(@Param("prefix") String prefix);
}
