package com.ces.erp.investor.repository;

import com.ces.erp.investor.entity.Investor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    List<Investor> findAllByDeletedFalse();

    Optional<Investor> findByIdAndDeletedFalse(Long id);

    boolean existsByVoenAndDeletedFalse(String voen);

    boolean existsByVoenAndIdNotAndDeletedFalse(String voen, Long id);

    List<Investor> findAllByDeletedTrue();

    long countByDeletedFalse();
}
