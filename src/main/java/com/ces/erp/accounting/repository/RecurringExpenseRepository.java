package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findAllByDeletedFalseOrderByCategoryKeyAscNameAsc();

    List<RecurringExpense> findAllByDeletedFalseAndActiveTrueOrderByCategoryKeyAscNameAsc();

    Optional<RecurringExpense> findByIdAndDeletedFalse(Long id);
}
