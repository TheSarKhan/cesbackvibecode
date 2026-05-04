package com.ces.erp.hr.repository;

import com.ces.erp.enums.PayrollStatus;
import com.ces.erp.hr.entity.PayrollPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    List<PayrollPeriod> findAllByDeletedFalseOrderByYearDescMonthDesc();

    Optional<PayrollPeriod> findByIdAndDeletedFalse(Long id);

    Optional<PayrollPeriod> findByYearAndMonthAndDeletedFalse(Integer year, Integer month);

    boolean existsByYearAndMonthAndDeletedFalse(Integer year, Integer month);

    @Query("""
        SELECT p FROM PayrollPeriod p
        WHERE p.deleted = false
          AND (:year IS NULL OR p.year = :year)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.year DESC, p.month DESC
        """)
    Page<PayrollPeriod> searchPaged(@Param("year") Integer year,
                                    @Param("status") PayrollStatus status,
                                    Pageable pageable);
}
