package com.ces.erp.accounting.repository;

import com.ces.erp.accounting.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.deleted = false ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findAllActive();

    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.deleted = false")
    Optional<Transaction> findByIdActive(Long id);
}
