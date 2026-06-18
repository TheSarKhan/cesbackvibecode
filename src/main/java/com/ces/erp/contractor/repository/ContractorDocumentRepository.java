package com.ces.erp.contractor.repository;

import com.ces.erp.contractor.entity.ContractorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractorDocumentRepository extends JpaRepository<ContractorDocument, Long> {

    Optional<ContractorDocument> findByIdAndContractorId(Long id, Long contractorId);

    List<ContractorDocument> findAllByContractorIdAndDeletedFalseOrderByCreatedAtDesc(Long contractorId);
}
