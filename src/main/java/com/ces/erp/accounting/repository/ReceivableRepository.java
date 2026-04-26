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

    @Query("SELECT r FROM Receivable r WHERE r.deleted = false " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (LOWER(r.project.projectCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Receivable> findAllWithFilters(@Param("status") ReceivableStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);

    Optional<Receivable> findByProjectIdAndDeletedFalse(Long projectId);

    Optional<Receivable> findByIdAndDeletedFalse(Long id);

    List<Receivable> findAllByDueDateBeforeAndStatusInAndDeletedFalse(LocalDate dueDate, List<ReceivableStatus> statuses);
}
