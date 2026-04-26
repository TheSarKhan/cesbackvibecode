package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
            SELECT i FROM Invoice i LEFT JOIN FETCH i.project LEFT JOIN FETCH i.contractor
            WHERE i.deleted = false
            AND i.status IN :statuses
            AND (CAST(:search AS string) IS NULL OR LOWER(COALESCE(i.invoiceNumber, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(COALESCE(i.notes, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (CAST(:type AS string) IS NULL OR i.type = :type)
            """,
            countQuery = """
            SELECT COUNT(i) FROM Invoice i
            WHERE i.deleted = false
            AND i.status IN :statuses
            AND (CAST(:search AS string) IS NULL OR LOWER(COALESCE(i.invoiceNumber, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(COALESCE(i.notes, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (CAST(:type AS string) IS NULL OR i.type = :type)
            """)
    Page<Invoice> findAllFiltered(@Param("search") String search,
                                  @Param("type") InvoiceType type,
                                  @Param("statuses") List<InvoiceStatus> statuses,
                                  Pageable pageable);

    @Query(value = """
            SELECT i FROM Invoice i LEFT JOIN FETCH i.project LEFT JOIN FETCH i.contractor
            WHERE i.deleted = false
            AND i.status IN :statuses
            AND i.type IN :types
            AND (CAST(:search AS string) IS NULL OR LOWER(COALESCE(i.invoiceNumber, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(COALESCE(i.notes, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(i) FROM Invoice i
            WHERE i.deleted = false
            AND i.status IN :statuses
            AND i.type IN :types
            AND (CAST(:search AS string) IS NULL OR LOWER(COALESCE(i.invoiceNumber, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(COALESCE(i.notes, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Invoice> findAllFilteredByTypes(@Param("search") String search,
                                         @Param("types") List<InvoiceType> types,
                                         @Param("statuses") List<InvoiceStatus> statuses,
                                         Pageable pageable);

    List<Invoice> findAllByProjectIdAndDeletedFalse(Long projectId);

    boolean existsByEtaxesIdAndDeletedFalse(String etaxesId);

    boolean existsByProjectIdAndStatusAndDeletedFalse(Long projectId, com.ces.erp.enums.InvoiceStatus status);

    boolean existsByProjectIdAndTypeAndPeriodMonthAndPeriodYearAndDeletedFalse(
            Long projectId, InvoiceType type, Integer periodMonth, Integer periodYear);

    // RETURNED olmayan aktiv qaimə var mı? (avtomatik yaratma üçün duplicate yoxlaması)
    boolean existsByProjectIdAndTypeAndPeriodMonthAndPeriodYearAndStatusNotAndDeletedFalse(
            Long projectId, InvoiceType type, Integer periodMonth, Integer periodYear,
            com.ces.erp.enums.InvoiceStatus status);

    // Mənbə gəlir qaiməsinə bağlı xərc qaimələri
    List<Invoice> findAllBySourceInvoiceIdAndDeletedFalse(Long sourceInvoiceId);

    List<Invoice> findAllByDeletedTrue();

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.project
            LEFT JOIN FETCH i.contractor
            WHERE i.deleted = false
            AND (CAST(:startDate as date) IS NULL OR i.invoiceDate >= :startDate)
            AND (CAST(:endDate as date) IS NULL OR i.invoiceDate <= :endDate)
            ORDER BY i.invoiceDate DESC, i.createdAt DESC
            """)
    List<Invoice> findAllActiveWithDateRange(@Param("startDate") java.time.LocalDate startDate,
                                             @Param("endDate") java.time.LocalDate endDate);
}
