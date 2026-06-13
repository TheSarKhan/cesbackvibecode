package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeductionTypeRepository extends JpaRepository<DeductionType, Long> {

    List<DeductionType> findAllByDeletedFalseOrderByDisplayOrderAscIdAsc();

    Optional<DeductionType> findByIdAndDeletedFalse(Long id);

    Optional<DeductionType> findByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
}
