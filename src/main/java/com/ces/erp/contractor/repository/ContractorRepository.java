package com.ces.erp.contractor.repository;

import com.ces.erp.contractor.entity.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractorRepository extends JpaRepository<Contractor, Long> {

    List<Contractor> findAllByDeletedFalse();

    Optional<Contractor> findByIdAndDeletedFalse(Long id);

    boolean existsByVoenAndDeletedFalse(String voen);

    boolean existsByVoenAndIdNotAndDeletedFalse(String voen, Long id);

    List<Contractor> findAllByDeletedTrue();

    long countByDeletedFalse();

    long countByDeletedTrue();
}
