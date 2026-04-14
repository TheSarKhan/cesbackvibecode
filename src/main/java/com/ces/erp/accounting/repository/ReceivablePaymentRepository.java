package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.ReceivablePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceivablePaymentRepository extends JpaRepository<ReceivablePayment, Long> {
    List<ReceivablePayment> findAllByReceivableIdAndDeletedFalseOrderByPaymentDateAsc(Long receivableId);
    Optional<ReceivablePayment> findByIdAndReceivableIdAndDeletedFalse(Long id, Long receivableId);
}
