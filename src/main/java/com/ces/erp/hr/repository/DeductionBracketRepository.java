package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.DeductionBracket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeductionBracketRepository extends JpaRepository<DeductionBracket, Long> {

    @Query("""
            SELECT b FROM DeductionBracket b
            JOIN FETCH b.deductionType t
            WHERE b.version.id = :versionId AND b.deleted = false AND t.deleted = false
            ORDER BY t.displayOrder ASC, b.party ASC, b.sortOrder ASC
            """)
    List<DeductionBracket> findByVersionWithType(Long versionId);

    List<DeductionBracket> findAllByVersionIdAndDeletedFalseOrderBySortOrderAsc(Long versionId);
}
