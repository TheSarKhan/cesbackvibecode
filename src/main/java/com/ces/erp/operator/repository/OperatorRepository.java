package com.ces.erp.operator.repository;

import com.ces.erp.operator.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    @Query("SELECT o FROM Operator o LEFT JOIN FETCH o.documents WHERE o.deleted = false ORDER BY o.lastName, o.firstName")
    List<Operator> findAllActive();

    @Query("SELECT o FROM Operator o LEFT JOIN FETCH o.documents WHERE o.id = :id AND o.deleted = false")
    Optional<Operator> findByIdActive(Long id);

    List<Operator> findAllByDeletedTrue();

    long countByDeletedFalse();

    long countByDeletedTrue();
}
