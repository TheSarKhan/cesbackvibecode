package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.TransactionRequest;
import com.ces.erp.accounting.dto.TransactionResponse;
import com.ces.erp.accounting.entity.Transaction;
import com.ces.erp.accounting.repository.TransactionRepository;
import com.ces.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAllActive().stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return TransactionResponse.from(findOrThrow(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest req) {
        Transaction t = Transaction.builder()
                .type(req.getType())
                .category(req.getCategory())
                .amount(req.getAmount())
                .transactionDate(req.getTransactionDate())
                .paymentMethod(req.getPaymentMethod())
                .referenceNumber(req.getReferenceNumber())
                .description(req.getDescription())
                .projectId(req.getProjectId())
                .contractorId(req.getContractorId())
                .customerId(req.getCustomerId())
                .notes(req.getNotes())
                .build();
        return TransactionResponse.from(transactionRepository.save(t));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest req) {
        Transaction t = findOrThrow(id);
        t.setType(req.getType());
        t.setCategory(req.getCategory());
        t.setAmount(req.getAmount());
        t.setTransactionDate(req.getTransactionDate());
        t.setPaymentMethod(req.getPaymentMethod());
        t.setReferenceNumber(req.getReferenceNumber());
        t.setDescription(req.getDescription());
        t.setProjectId(req.getProjectId());
        t.setContractorId(req.getContractorId());
        t.setCustomerId(req.getCustomerId());
        t.setNotes(req.getNotes());
        return TransactionResponse.from(transactionRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        Transaction t = findOrThrow(id);
        t.softDelete();
        transactionRepository.save(t);
    }

    private Transaction findOrThrow(Long id) {
        return transactionRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Əməliyyat", id));
    }
}
