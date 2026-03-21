package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.enums.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.project
            LEFT JOIN FETCH i.contractor
            WHERE i.deleted = false
            ORDER BY i.invoiceDate DESC, i.createdAt DESC
            """)
    List<Invoice> findAllActive();

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.project
            LEFT JOIN FETCH i.contractor
            WHERE i.id = :id AND i.deleted = false
            """)
    Optional<Invoice> findByIdActive(Long id);

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.project
            LEFT JOIN FETCH i.contractor
            WHERE i.type = :type AND i.deleted = false
            ORDER BY i.invoiceDate DESC
            """)
    List<Invoice> findAllByType(InvoiceType type);

    List<Invoice> findAllByProjectIdAndDeletedFalse(Long projectId);

    boolean existsByEtaxesIdAndDeletedFalse(String etaxesId);

    List<Invoice> findAllByDeletedTrue();
}
