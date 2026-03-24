package com.ces.erp.operator.repository;

import com.ces.erp.operator.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    @Query("SELECT o FROM Operator o LEFT JOIN FETCH o.documents WHERE o.deleted = false ORDER BY o.lastName, o.firstName")
    List<Operator> findAllActive();

    /** Hal-hazırda PENDING və ya ACTIVE layihəyə təyin olunmuş operator ID-ləri */
    @Query("""
            SELECT DISTINCT op.id FROM Operator op
            WHERE EXISTS (
                SELECT p FROM CoordinatorPlan p
                WHERE p.operator = op
                  AND p.deleted = false
                  AND EXISTS (
                      SELECT proj FROM Project proj
                      WHERE proj.request = p.request
                        AND proj.deleted = false
                        AND proj.status IN ('PENDING', 'ACTIVE')
                  )
            )
            """)
    Set<Long> findBusyOperatorIds();

    @Query("SELECT o FROM Operator o LEFT JOIN FETCH o.documents WHERE o.id = :id AND o.deleted = false")
    Optional<Operator> findByIdActive(Long id);

    List<Operator> findAllByDeletedTrue();

    long countByDeletedFalse();

    long countByDeletedTrue();
}
