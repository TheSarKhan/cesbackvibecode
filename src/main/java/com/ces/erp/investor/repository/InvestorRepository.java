package com.ces.erp.investor.repository;

import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import com.ces.erp.investor.entity.Investor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    List<Investor> findAllByDeletedFalse();

    @Query("SELECT i FROM Investor i WHERE i.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(i.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(i.voen, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(i.contactPerson, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (:status IS NULL OR i.status = :status)" +
            " AND (:riskLevel IS NULL OR i.riskLevel = :riskLevel)")
    Page<Investor> findAllFiltered(@Param("search") String search,
                                   @Param("status") ContractorStatus status,
                                   @Param("riskLevel") RiskLevel riskLevel,
                                   Pageable pageable);

    Optional<Investor> findByIdAndDeletedFalse(Long id);

    boolean existsByVoenAndDeletedFalse(String voen);

    boolean existsByVoenAndIdNotAndDeletedFalse(String voen, Long id);

    List<Investor> findAllByDeletedTrue();

    long countByDeletedFalse();

    long countByDeletedTrue();
}
