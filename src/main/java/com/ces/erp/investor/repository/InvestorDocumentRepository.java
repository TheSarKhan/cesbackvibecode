package com.ces.erp.investor.repository;

import com.ces.erp.investor.entity.InvestorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestorDocumentRepository extends JpaRepository<InvestorDocument, Long> {

    Optional<InvestorDocument> findByIdAndInvestorId(Long id, Long investorId);

    List<InvestorDocument> findAllByInvestorIdAndDeletedFalseOrderByCreatedAtDesc(Long investorId);
}
