package com.ces.erp.operator.repository;

import com.ces.erp.enums.OperatorDocumentType;
import com.ces.erp.operator.entity.OperatorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorDocumentRepository extends JpaRepository<OperatorDocument, Long> {

    Optional<OperatorDocument> findByOperatorIdAndDocumentType(Long operatorId, OperatorDocumentType type);

    Optional<OperatorDocument> findByIdAndOperatorId(Long id, Long operatorId);
}
