package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.*;
import com.ces.erp.accounting.entity.RecurringExpense;
import com.ces.erp.accounting.repository.RecurringExpenseRepository;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.RecurrenceFrequency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private final RecurringExpenseRepository repository;
    private final InvoiceService invoiceService;

    // ─── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getAll() {
        return repository.findAllByDeletedFalseOrderByCategoryKeyAscNameAsc().stream()
                .map(RecurringExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getAllActive() {
        return repository.findAllByDeletedFalseAndActiveTrueOrderByCategoryKeyAscNameAsc().stream()
                .map(RecurringExpenseResponse::from)
                .toList();
    }

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public RecurringExpenseResponse create(RecurringExpenseRequest req) {
        RecurringExpense entity = RecurringExpense.builder()
                .name(req.getName().trim())
                .categoryKey(req.getCategoryKey().trim())
                .categoryLabel(req.getCategoryLabel().trim())
                .sourceKey(req.getSourceKey().trim())
                .sourceLabel(req.getSourceLabel().trim())
                .amount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO)
                .frequency(req.getFrequency() != null ? req.getFrequency() : RecurrenceFrequency.MONTHLY)
                .dayOfMonth(req.getDayOfMonth())
                .notes(req.getNotes())
                .active(req.isActive())
                .build();
        return RecurringExpenseResponse.from(repository.save(entity));
    }

    @Transactional
    public RecurringExpenseResponse update(Long id, RecurringExpenseRequest req) {
        RecurringExpense entity = findOrThrow(id);
        entity.setName(req.getName().trim());
        entity.setCategoryKey(req.getCategoryKey().trim());
        entity.setCategoryLabel(req.getCategoryLabel().trim());
        entity.setSourceKey(req.getSourceKey().trim());
        entity.setSourceLabel(req.getSourceLabel().trim());
        entity.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        entity.setFrequency(req.getFrequency() != null ? req.getFrequency() : RecurrenceFrequency.MONTHLY);
        entity.setDayOfMonth(req.getDayOfMonth());
        entity.setNotes(req.getNotes());
        entity.setActive(req.isActive());
        return RecurringExpenseResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        RecurringExpense entity = findOrThrow(id);
        entity.softDelete();
        repository.save(entity);
    }

    // ─── Qaimə Generasiyası ───────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse generateInvoice(Long id, GenerateFromRecurringRequest req) {
        RecurringExpense rec = findOrThrow(id);
        if (!rec.isActive()) {
            throw new BusinessException("Bu daimi ödəniş aktiv deyil");
        }

        BigDecimal amount = (req.getAmountOverride() != null && req.getAmountOverride().compareTo(BigDecimal.ZERO) > 0)
                ? req.getAmountOverride()
                : rec.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Məbləğ daxil edilməlidir");
        }

        InvoiceRequest invoiceReq = new InvoiceRequest();
        invoiceReq.setType(InvoiceType.COMPANY_EXPENSE);
        invoiceReq.setStatus(InvoiceStatus.SENT);           // Birbaşa gözləyən qaimələrə göndər
        invoiceReq.setAmount(amount);
        invoiceReq.setInvoiceDate(req.getInvoiceDate());
        invoiceReq.setCompanyName(rec.getSourceLabel());    // Məs: "Azercell"
        invoiceReq.setServiceDescription(rec.getCategoryLabel() + " — " + rec.getName());
        invoiceReq.setNotes(rec.getNotes());

        return invoiceService.create(invoiceReq);
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private RecurringExpense findOrThrow(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daimi ödəniş", id));
    }
}
