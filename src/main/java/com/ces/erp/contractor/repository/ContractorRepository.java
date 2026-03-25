package com.ces.erp.contractor.repository;

import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractorRepository extends JpaRepository<Contractor, Long> {

    List<Contractor> findAllByDeletedFalse();

    @Query("SELECT c FROM Contractor c WHERE c.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.voen, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.contactPerson, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (:status IS NULL OR c.status = :status)" +
            " AND (:riskLevel IS NULL OR c.riskLevel = :riskLevel)")
    Page<Contractor> findAllFiltered(@Param("search") String search,
                                     @Param("status") ContractorStatus status,
                                     @Param("riskLevel") RiskLevel riskLevel,
                                     Pageable pageable);

    Optional<Contractor> findByIdAndDeletedFalse(Long id);

    boolean existsByVoenAndDeletedFalse(String voen);

    boolean existsByVoenAndIdNotAndDeletedFalse(String voen, Long id);

    List<Contractor> findAllByDeletedTrue();

    long countByDeletedFalse();

    long countByDeletedTrue();
}
