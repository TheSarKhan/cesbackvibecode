package com.ces.erp.bank.repository;

import com.ces.erp.bank.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, Long> {

    List<Bank> findAllByDeletedFalseOrderByCreatedAtAsc();

    Optional<Bank> findByIdAndDeletedFalse(Long id);
}
