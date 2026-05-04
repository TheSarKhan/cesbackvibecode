package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.TaxRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxRateConfigRepository extends JpaRepository<TaxRateConfig, Long> {

    List<TaxRateConfig> findAllByDeletedFalseOrderByYearDesc();

    Optional<TaxRateConfig> findByIdAndDeletedFalse(Long id);

    Optional<TaxRateConfig> findByYearAndDeletedFalse(Integer year);

    Optional<TaxRateConfig> findFirstByActiveTrueAndDeletedFalseOrderByYearDesc();
}
