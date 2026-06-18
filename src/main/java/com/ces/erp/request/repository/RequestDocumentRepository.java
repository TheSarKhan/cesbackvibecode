package com.ces.erp.request.repository;

import com.ces.erp.request.entity.RequestDocument;
import com.ces.erp.request.entity.RequestDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestDocumentRepository extends JpaRepository<RequestDocument, Long> {

    // ─── Sənəd mərkəzi aqreqasiyası ──────────────────────────────────────────

    /** Müştəri tərəfi müqavilələri — bu müştərinin bütün sorğularından. */
    @Query("""
            SELECT d FROM RequestDocument d
            WHERE d.deleted = false
              AND d.request.customer.id = :customerId
              AND d.docType IN :types
            ORDER BY d.createdAt DESC
            """)
    List<RequestDocument> findCustomerSideDocs(@Param("customerId") Long customerId,
                                               @Param("types") List<RequestDocumentType> types);

    /** Sahib tərəfi müqavilələri — bu podratçının iştirak etdiyi bütün xətlərdən. */
    @Query("""
            SELECT d FROM RequestDocument d
            WHERE d.deleted = false
              AND d.planItem.contractor.id = :contractorId
              AND d.docType IN :types
            ORDER BY d.createdAt DESC
            """)
    List<RequestDocument> findContractorSideDocs(@Param("contractorId") Long contractorId,
                                                 @Param("types") List<RequestDocumentType> types);

    /** Sahib tərəfi müqavilələri — bu investorun iştirak etdiyi bütün xətlərdən. */
    @Query("""
            SELECT d FROM RequestDocument d
            WHERE d.deleted = false
              AND d.planItem.investor.id = :investorId
              AND d.docType IN :types
            ORDER BY d.createdAt DESC
            """)
    List<RequestDocument> findInvestorSideDocs(@Param("investorId") Long investorId,
                                               @Param("types") List<RequestDocumentType> types);

    List<RequestDocument> findAllByRequestIdAndDeletedFalse(Long requestId);

    Optional<RequestDocument> findByRequestIdAndDocTypeAndDeletedFalse(Long requestId, RequestDocumentType docType);

    boolean existsByRequestIdAndDocTypeAndDeletedFalse(Long requestId, RequestDocumentType docType);

    // Çoxlu texnika: konkret xətt üzrə sənəd
    Optional<RequestDocument> findByRequestIdAndDocTypeAndPlanItemIdAndDeletedFalse(
            Long requestId, RequestDocumentType docType, Long planItemId);

    // Sorğu səviyyəli (xəttə bağlı olmayan) sənəd
    Optional<RequestDocument> findByRequestIdAndDocTypeAndPlanItemIsNullAndDeletedFalse(
            Long requestId, RequestDocumentType docType);

    boolean existsByRequestIdAndDocTypeAndPlanItemIdAndDeletedFalse(
            Long requestId, RequestDocumentType docType, Long planItemId);
}
