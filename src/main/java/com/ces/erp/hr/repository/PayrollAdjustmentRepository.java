package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.PayrollAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, Long> {
    List<PayrollAdjustment> findAllByEntryIdAndDeletedFalse(Long entryId);
}
