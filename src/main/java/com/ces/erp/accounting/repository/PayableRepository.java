package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.Payable;
import com.ces.erp.enums.PayableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayableRepository extends JpaRepository<Payable, Long> {

    @Query("SELECT p FROM Payable p LEFT JOIN p.contractor c WHERE p.deleted = false " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:search = '' OR LOWER(p.project.projectCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.companyName, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(p.investorName, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Payable> findAllWithFilters(@Param("status") PayableStatus status,
                                     @Param("search") String search,
                                     Pageable pageable);

    // Çoxlu sahib: 1 layihədə bir neçə payable ola bilər
    List<Payable> findAllByProjectIdAndDeletedFalse(Long projectId);

    Optional<Payable> findByIdAndDeletedFalse(Long id);

    List<Payable> findAllByDueDateBeforeAndStatusInAndDeletedFalse(
            LocalDate dueDate, List<PayableStatus> statuses);

    @Query("SELECT p FROM Payable p LEFT JOIN FETCH p.payments LEFT JOIN FETCH p.project WHERE p.contractor.id = :contractorId AND p.deleted = false ORDER BY p.dueDate DESC")
    List<Payable> findAllByContractorId(@Param("contractorId") Long contractorId);

    @Query("SELECT p FROM Payable p LEFT JOIN FETCH p.payments LEFT JOIN FETCH p.project WHERE p.investorVoen = :voen AND p.deleted = false ORDER BY p.dueDate DESC")
    List<Payable> findAllByInvestorVoen(@Param("voen") String voen);
}
