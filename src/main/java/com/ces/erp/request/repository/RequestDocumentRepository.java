package com.ces.erp.request.repository;

import com.ces.erp.request.entity.RequestDocument;
import com.ces.erp.request.entity.RequestDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestDocumentRepository extends JpaRepository<RequestDocument, Long> {

    List<RequestDocument> findAllByRequestIdAndDeletedFalse(Long requestId);

    Optional<RequestDocument> findByRequestIdAndDocTypeAndDeletedFalse(Long requestId, RequestDocumentType docType);

    boolean existsByRequestIdAndDocTypeAndDeletedFalse(Long requestId, RequestDocumentType docType);
}
