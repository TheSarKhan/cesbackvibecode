package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.AccountingSummaryResponse;
import com.ces.erp.accounting.dto.InvoiceRequest;
import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.websocket.NotificationService;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService implements ApprovalHandler {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final ContractorRepository contractorRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override public String getEntityType() { return "INVOICE"; }
    @Override public String getModuleCode()  { return "ACCOUNTING"; }
    @Override public String getLabel(Long id) {
        Invoice inv = findOrThrow(id);
        return inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "Qaimə #" + id;
    }
    @Override public Object getSnapshot(Long id) { return InvoiceResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            InvoiceRequest req = objectMapper.readValue(json, InvoiceRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    // ─── List & Summary ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAllActive().stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByType(InvoiceType type) {
        return invoiceRepository.findAllByType(type).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getAllPaged(int page, int size, String search, String type) {
        String q = (search != null && !search.isBlank()) ? search : null;
        InvoiceType t = (type != null && !type.isBlank()) ? InvoiceType.valueOf(type) : null;
        var pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending().and(Sort.by("createdAt").descending()));
        return PagedResponse.from(invoiceRepository.findAllFiltered(q, t, pageable), InvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return InvoiceResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public AccountingSummaryResponse getSummary() {
        List<Invoice> all = invoiceRepository.findAllActive();

        BigDecimal totalIncome = sum(all, InvoiceType.INCOME);
        BigDecimal totalContractor = sum(all, InvoiceType.CONTRACTOR_EXPENSE);
        BigDecimal totalCompany = sum(all, InvoiceType.COMPANY_EXPENSE);
        BigDecimal totalExpense = totalContractor.add(totalCompany);

        return AccountingSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalContractorExpense(totalContractor)
                .totalCompanyExpense(totalCompany)
                .totalExpense(totalExpense)
                .netProfit(totalIncome.subtract(totalExpense))
                .incomeCount(count(all, InvoiceType.INCOME))
                .contractorExpenseCount(count(all, InvoiceType.CONTRACTOR_EXPENSE))
                .companyExpenseCount(count(all, InvoiceType.COMPANY_EXPENSE))
                .build();
    }

    // ─── Create / Update / Delete ─────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest req) {
        validate(req, null);

        Invoice inv = Invoice.builder()
                .type(req.getType())
                .invoiceNumber(req.getInvoiceNumber())
                .amount(req.getAmount())
                .invoiceDate(req.getInvoiceDate())
                .etaxesId(req.getEtaxesId())
                .equipmentName(req.getEquipmentName())
                .companyName(req.getCompanyName())
                .serviceDescription(req.getServiceDescription())
                .notes(req.getNotes())
                .build();

        if (req.getProjectId() != null) {
            inv.setProject(projectRepository.findById(req.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Layihə", req.getProjectId())));
        }
        if (req.getContractorId() != null) {
            inv.setContractor(contractorRepository.findById(req.getContractorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Podratçı", req.getContractorId())));
        }

        Invoice saved = invoiceRepository.save(inv);
        notificationService.success("Yeni faktura", "Faktura yaradıldı: " + saved.getInvoiceNumber(), "ACCOUNTING");
        auditService.log("FAKTURA", saved.getId(), saved.getInvoiceNumber(), "YARADILDI", "Yeni faktura qeydiyyatı");
        return InvoiceResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "ACCOUNTING", entityType = "INVOICE")
    public InvoiceResponse update(Long id, InvoiceRequest req) {
        Invoice inv = findOrThrow(id);
        validate(req, id);

        inv.setType(req.getType());
        inv.setInvoiceNumber(req.getInvoiceNumber());
        inv.setAmount(req.getAmount());
        inv.setInvoiceDate(req.getInvoiceDate());
        inv.setEtaxesId(req.getEtaxesId());
        inv.setEquipmentName(req.getEquipmentName());
        inv.setCompanyName(req.getCompanyName());
        inv.setServiceDescription(req.getServiceDescription());
        inv.setNotes(req.getNotes());

        inv.setProject(req.getProjectId() != null
                ? projectRepository.findById(req.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Layihə", req.getProjectId()))
                : null);
        inv.setContractor(req.getContractorId() != null
                ? contractorRepository.findById(req.getContractorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Podratçı", req.getContractorId()))
                : null);

        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "YENİLƏNDİ", "Faktura məlumatları yeniləndi");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    @RequiresApproval(module = "ACCOUNTING", entityType = "INVOICE", isDelete = true)
    public void delete(Long id) {
        Invoice inv = findOrThrow(id);
        auditService.log("FAKTURA", inv.getId(), inv.getInvoiceNumber(), "SİLİNDİ", "Faktura silindi");
        inv.softDelete();
        invoiceRepository.save(inv);
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private void validate(InvoiceRequest req, Long excludeId) {
        if (req.getType() == InvoiceType.INCOME || req.getType() == InvoiceType.CONTRACTOR_EXPENSE) {
            if (req.getProjectId() == null) {
                throw new BusinessException("Bu qaimə növü üçün layihə seçilməlidir");
            }
        }
        if (req.getType() == InvoiceType.CONTRACTOR_EXPENSE && req.getContractorId() == null) {
            throw new BusinessException("B1 qaiməsi üçün podratçı seçilməlidir");
        }
        if (req.getType() == InvoiceType.INCOME && req.getEtaxesId() != null && !req.getEtaxesId().isBlank()) {
            boolean exists = invoiceRepository.existsByEtaxesIdAndDeletedFalse(req.getEtaxesId());
            if (exists && excludeId == null) {
                throw new BusinessException("Bu ETaxes ID artıq mövcuddur: " + req.getEtaxesId());
            }
        }
    }

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Qaimə", id));
    }

    private BigDecimal sum(List<Invoice> list, InvoiceType type) {
        return list.stream()
                .filter(i -> i.getType() == type)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(List<Invoice> list, InvoiceType type) {
        return list.stream().filter(i -> i.getType() == type).count();
    }
}
