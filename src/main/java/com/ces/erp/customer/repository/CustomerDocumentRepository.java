package com.ces.erp.customer.repository;

import com.ces.erp.customer.entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {

    Optional<CustomerDocument> findByIdAndCustomerId(Long id, Long customerId);
}
