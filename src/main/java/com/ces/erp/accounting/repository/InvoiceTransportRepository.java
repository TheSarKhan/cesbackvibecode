package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.InvoiceTransport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceTransportRepository extends JpaRepository<InvoiceTransport, Long> {
    List<InvoiceTransport> findAllByInvoiceIdAndDeletedFalse(Long invoiceId);
    void deleteAllByInvoiceId(Long invoiceId);
}
