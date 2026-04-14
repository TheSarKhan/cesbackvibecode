package com.ces.erp.investor.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.enums.RiskLevel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.investor.dto.InvestorRequest;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestorService implements ApprovalHandler {

    private final InvestorRepository investorRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Override public String getEntityType() { return "INVESTOR"; }
    @Override public String getModuleCode()  { return "INVESTORS"; }
    @Override public String getLabel(Long id) { return findOrThrow(id).getCompanyName(); }
    @Override public Object getSnapshot(Long id) { return InvestorResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            InvestorRequest req = objectMapper.readValue(json, InvestorRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<InvestorResponse> getAll() {
        return investorRepository.findAllByDeletedFalse().stream()
                .map(InvestorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvestorResponse> getAllPaged(int page, int size, String search, String status, String riskLevel) {
        String q = (search != null && !search.isBlank()) ? search : null;
        String s = (status != null && !status.isBlank()) ? status : null;
        String r = (riskLevel != null && !riskLevel.isBlank()) ? riskLevel : null;
        var pageable = PageRequest.of(page, size, Sort.by("companyName").ascending());
        return PagedResponse.from(investorRepository.findAllFiltered(q, s, r, pageable), InvestorResponse::from);
    }

    public InvestorResponse getById(Long id) {
        return InvestorResponse.from(findOrThrow(id));
    }

    @Transactional
    public InvestorResponse create(InvestorRequest request) {
        if (investorRepository.existsByVoenAndDeletedFalse(request.getVoen())) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        Investor saved = investorRepository.save(toEntity(request, new Investor()));
        auditService.log("İNVESTOR", saved.getId(), saved.getCompanyName(), "YARADILDI", "Yeni investor qeydiyyatı");
        return InvestorResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "INVESTORS", entityType = "INVESTOR")
    public InvestorResponse update(Long id, InvestorRequest request) {
        Investor investor = findOrThrow(id);
        if (investorRepository.existsByVoenAndIdNotAndDeletedFalse(request.getVoen(), id)) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        Investor updated = investorRepository.save(toEntity(request, investor));
        auditService.log("İNVESTOR", updated.getId(), updated.getCompanyName(), "YENİLƏNDİ", "İnvestor məlumatları yeniləndi");
        return InvestorResponse.from(updated);
    }

    @Transactional
    @RequiresApproval(module = "INVESTORS", entityType = "INVESTOR", isDelete = true)
    public void delete(Long id) {
        Investor investor = findOrThrow(id);
        auditService.log("İNVESTOR", investor.getId(), investor.getCompanyName(), "SİLİNDİ", "İnvestor silindi");
        investor.softDelete();
        investorRepository.save(investor);
    }

    @Transactional
    public void deleteAll(List<Long> ids) {
        for (Long id : ids) {
            Investor investor = findOrThrow(id);
            auditService.log("İNVESTOR", investor.getId(), investor.getCompanyName(), "SİLİNDİ", "Toplu silmə");
            investor.softDelete();
            investorRepository.save(investor);
        }
    }

    private Investor findOrThrow(Long id) {
        return investorRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İnvestor", id));
    }

    private Investor toEntity(InvestorRequest r, Investor i) {
        i.setCompanyName(r.getCompanyName());
        i.setVoen(r.getVoen());
        i.setContactPerson(r.getContactPerson());
        i.setContactPhone(r.getContactPhone());
        i.setAddress(r.getAddress());
        i.setPaymentType(r.getPaymentType());
        i.setStatus(r.getStatus());
        i.setRating(r.getRating() != null ? r.getRating() : java.math.BigDecimal.ZERO);
        i.setRiskLevel(r.getRiskLevel());
        i.setNotes(r.getNotes());
        return i;
    }
}
