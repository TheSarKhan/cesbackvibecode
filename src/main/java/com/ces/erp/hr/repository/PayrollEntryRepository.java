package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.PayrollEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollEntryRepository extends JpaRepository<PayrollEntry, Long> {

    List<PayrollEntry> findAllByPeriodIdAndDeletedFalseOrderByEmployeeFullNameAsc(Long periodId);

    Optional<PayrollEntry> findByIdAndDeletedFalse(Long id);

    Optional<PayrollEntry> findByPeriodIdAndEmployeeIdAndDeletedFalse(Long periodId, Long employeeId);

    List<PayrollEntry> findAllByEmployeeIdAndDeletedFalseOrderByCreatedAtDesc(Long employeeId);

    long countByPeriodIdAndDeletedFalse(Long periodId);
}
