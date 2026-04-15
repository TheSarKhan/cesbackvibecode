package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.PayablePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayablePaymentRepository extends JpaRepository<PayablePayment, Long> {

    List<PayablePayment> findAllByPayableIdAndDeletedFalseOrderByPaymentDateAsc(Long payableId);

    Optional<PayablePayment> findByIdAndPayableIdAndDeletedFalse(Long id, Long payableId);
}
