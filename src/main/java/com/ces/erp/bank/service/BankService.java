package com.ces.erp.bank.service;

import com.ces.erp.bank.dto.BankRequest;
import com.ces.erp.bank.dto.BankResponse;
import com.ces.erp.bank.entity.Bank;
import com.ces.erp.bank.repository.BankRepository;
import com.ces.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository repository;

    public List<BankResponse> getAll() {
        return repository.findAllByDeletedFalseOrderByCreatedAtAsc()
                .stream().map(BankResponse::from).toList();
    }

    @Transactional
    public BankResponse create(BankRequest req) {
        Bank bank = Bank.builder()
                .bankName(req.getBankName())
                .bankCode(req.getBankCode())
                .swift(req.getSwift())
                .iban(req.getIban())
                .correspondentAccount(req.getCorrespondentAccount())
                .settlementAccount(req.getSettlementAccount())
                .build();
        return BankResponse.from(repository.save(bank));
    }

    @Transactional
    public BankResponse update(Long id, BankRequest req) {
        Bank bank = findOrThrow(id);
        bank.setBankName(req.getBankName());
        bank.setBankCode(req.getBankCode());
        bank.setSwift(req.getSwift());
        bank.setIban(req.getIban());
        bank.setCorrespondentAccount(req.getCorrespondentAccount());
        bank.setSettlementAccount(req.getSettlementAccount());
        return BankResponse.from(repository.save(bank));
    }

    @Transactional
    public void delete(Long id) {
        Bank bank = findOrThrow(id);
        bank.softDelete();
        repository.save(bank);
    }

    private Bank findOrThrow(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank", id));
    }
}
