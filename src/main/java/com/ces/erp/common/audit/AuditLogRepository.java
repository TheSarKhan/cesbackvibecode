package com.ces.erp.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a ORDER BY a.performedAt DESC LIMIT 50")
    List<AuditLog> findRecent();

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.performedAt DESC")
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:entityType IS NULL OR a.entityType = :entityType)
          AND (:action     IS NULL OR a.action = :action)
          AND (:q          IS NULL OR LOWER(a.entityLabel) LIKE %:q% OR LOWER(a.performedBy) LIKE %:q%)
          AND a.performedAt >= :from
          AND a.performedAt <= :to
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findFiltered(
            @Param("entityType") String entityType,
            @Param("action")     String action,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable
    );
}
