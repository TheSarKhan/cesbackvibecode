package com.ces.erp.expense.repository;

import com.ces.erp.expense.entity.ExpenseSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseSourceRepository extends JpaRepository<ExpenseSource, Long> {

    @Query("SELECT s FROM ExpenseSource s JOIN FETCH s.category WHERE s.deleted = false ORDER BY s.name ASC")
    List<ExpenseSource> findAllActive();

    @Query("SELECT s FROM ExpenseSource s JOIN FETCH s.category WHERE s.deleted = false AND s.active = true ORDER BY s.name ASC")
    List<ExpenseSource> findAllActiveAndEnabled();

    @Query("SELECT s FROM ExpenseSource s JOIN FETCH s.category WHERE s.category.id = :categoryId AND s.deleted = false ORDER BY s.name ASC")
    List<ExpenseSource> findAllByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT s FROM ExpenseSource s JOIN FETCH s.category WHERE s.category.id = :categoryId AND s.deleted = false AND s.active = true ORDER BY s.name ASC")
    List<ExpenseSource> findAllByCategoryIdAndActiveTrue(@Param("categoryId") Long categoryId);

    @Query("SELECT s FROM ExpenseSource s JOIN FETCH s.category WHERE s.id = :id AND s.deleted = false")
    Optional<ExpenseSource> findByIdAndDeletedFalse(@Param("id") Long id);

    boolean existsByCodeAndCategoryIdAndDeletedFalse(String code, Long categoryId);

    boolean existsByCodeAndCategoryIdAndIdNotAndDeletedFalse(String code, Long categoryId, Long id);

    List<ExpenseSource> findAllByDeletedTrue();
}
