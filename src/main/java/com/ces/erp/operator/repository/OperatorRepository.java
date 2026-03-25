package com.ces.erp.operator.repository;

import com.ces.erp.operator.entity.Operator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    @Query("SELECT o FROM Operator o LEFT JOIN FETCH o.documents WHERE o.deleted = false ORDER BY o.lastName, o.firstName")
    List<Operator> findAllActive();

    @Query(value = "SELECT o FROM Operator o WHERE o.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(o.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(o.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(o.email, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(o.specialization, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
            countQuery = "SELECT COUNT(o) FROM Operator o WHERE o.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(o.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(o.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(o.email, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(o.specialization, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Operator> findAllFiltered(@Param("search") String search, Pageable pageable);

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
