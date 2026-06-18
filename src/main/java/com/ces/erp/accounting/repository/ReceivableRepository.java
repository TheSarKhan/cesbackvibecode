package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.Receivable;
import com.ces.erp.enums.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    @Query("SELECT r FROM Receivable r LEFT JOIN r.customer cu LEFT JOIN r.project pr WHERE r.deleted = false " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:search = '' OR LOWER(COALESCE(pr.projectCode, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(cu.companyName, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Receivable> findAllWithFilters(@Param("status") ReceivableStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);

    Optional<Receivable> findByProjectIdAndDeletedFalse(Long projectId);

    Optional<Receivable> findByIdAndDeletedFalse(Long id);

    List<Receivable> findAllByDueDateBeforeAndStatusInAndDeletedFalse(LocalDate dueDate, List<ReceivableStatus> statuses);

    @Query("SELECT r FROM Receivable r LEFT JOIN FETCH r.payments LEFT JOIN FETCH r.project WHERE r.customer.id = :customerId AND r.deleted = false ORDER BY r.dueDate DESC")
    List<Receivable> findAllByCustomerId(@Param("customerId") Long customerId);
}
