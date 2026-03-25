package com.ces.erp.customer.repository;

import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.deleted = false")
    List<Customer> findAllByDeletedFalse();

    @Query("SELECT c FROM Customer c WHERE c.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.voen, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.supplierPerson, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.officeContactPerson, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (CAST(:status AS string) IS NULL OR c.status = :status)" +
            " AND (CAST(:riskLevel AS string) IS NULL OR c.riskLevel = :riskLevel)")
    Page<Customer> findAllFiltered(@Param("search") String search,
                                   @Param("status") CustomerStatus status,
                                   @Param("riskLevel") RiskLevel riskLevel,
                                   Pageable pageable);

    Optional<Customer> findByIdAndDeletedFalse(Long id);

    boolean existsByVoenAndDeletedFalse(String voen);

    boolean existsByVoenAndIdNotAndDeletedFalse(String voen, @Param("id") Long id);

    List<Customer> findAllByDeletedTrue();

    long countByDeletedFalse();

    long countByDeletedTrue();
}
