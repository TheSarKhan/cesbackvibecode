package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    Optional<InvoiceLine> findByIdAndDeletedFalse(Long id);
}
