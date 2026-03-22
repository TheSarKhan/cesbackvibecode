package com.ces.erp.approval.repository;

import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.enums.OperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PendingOperationRepository extends JpaRepository<PendingOperation, Long> {

    boolean existsByEntityTypeAndEntityIdAndStatusAndDeletedFalse(
            String entityType, Long entityId, OperationStatus status);

    @Query("""
            SELECT p FROM PendingOperation p
            LEFT JOIN FETCH p.performedBy
            LEFT JOIN FETCH p.performerDepartment
            LEFT JOIN FETCH p.processedBy
            WHERE p.deleted = false
            AND p.performerDepartment.id IN :deptIds
            ORDER BY p.createdAt DESC
            """)
    List<PendingOperation> findAllByDepartmentIds(@Param("deptIds") List<Long> deptIds);

    @Query("""
            SELECT p FROM PendingOperation p
            LEFT JOIN FETCH p.performedBy
            LEFT JOIN FETCH p.performerDepartment
            LEFT JOIN FETCH p.processedBy
            WHERE p.deleted = false
            ORDER BY p.createdAt DESC
            """)
    List<PendingOperation> findAllActive();

    @Query("""
            SELECT p FROM PendingOperation p
            LEFT JOIN FETCH p.performedBy
            LEFT JOIN FETCH p.performerDepartment
            LEFT JOIN FETCH p.processedBy
            WHERE p.id = :id AND p.deleted = false
            """)
    Optional<PendingOperation> findByIdActive(@Param("id") Long id);

    long countByStatusAndDeletedFalse(OperationStatus status);
}
