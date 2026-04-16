package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.GeneratedDocument;
import com.ces.erp.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    Optional<GeneratedDocument> findByIdAndDeletedFalse(Long id);

    @Query(value = """
            SELECT d FROM GeneratedDocument d
            WHERE d.deleted = false
            AND (CAST(:search AS string) IS NULL
              OR LOWER(d.customerName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              OR d.documentNumber LIKE CONCAT('%', CAST(:search AS string), '%'))
            AND (CAST(:type AS string) IS NULL OR d.documentType = :type)
            ORDER BY d.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(d) FROM GeneratedDocument d
            WHERE d.deleted = false
            AND (CAST(:search AS string) IS NULL
              OR LOWER(d.customerName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              OR d.documentNumber LIKE CONCAT('%', CAST(:search AS string), '%'))
            AND (CAST(:type AS string) IS NULL OR d.documentType = :type)
            """)
    Page<GeneratedDocument> findAllFiltered(@Param("search") String search,
                                            @Param("type") DocumentType type,
                                            Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(d.documentNumber AS int)), 0) FROM GeneratedDocument d")
    int findMaxDocumentNumber();
}
