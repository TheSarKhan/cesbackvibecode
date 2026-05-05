package com.ces.erp.expense.repository;

import com.ces.erp.expense.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findAllByDeletedFalseOrderByNameAsc();

    List<ExpenseCategory> findAllByDeletedFalseAndActiveTrueOrderByNameAsc();

    Optional<ExpenseCategory> findByIdAndDeletedFalse(Long id);

    boolean existsByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndDeletedFalse(String code, Long id);

    List<ExpenseCategory> findAllByDeletedTrue();
}
