package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.DeductionConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeductionConfigVersionRepository extends JpaRepository<DeductionConfigVersion, Long> {

    List<DeductionConfigVersion> findAllByDeletedFalseOrderByEffectiveDateDescVersionNoDesc();

    Optional<DeductionConfigVersion> findByIdAndDeletedFalse(Long id);

    Optional<DeductionConfigVersion> findFirstByActiveTrueAndDeletedFalseOrderByEffectiveDateDescVersionNoDesc();

    /** Verilən tarix üçün qüvvədə olan versiya: effective_date ≤ tarix olan ən son. */
    Optional<DeductionConfigVersion> findFirstByDeletedFalseAndEffectiveDateLessThanEqualOrderByEffectiveDateDescVersionNoDesc(LocalDate date);

    Optional<DeductionConfigVersion> findFirstByOrderByVersionNoDesc();
}
