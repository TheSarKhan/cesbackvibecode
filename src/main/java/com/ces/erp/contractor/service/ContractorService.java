package com.ces.erp.contractor.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.contractor.dto.ContractorRequest;
import com.ces.erp.contractor.dto.ContractorResponse;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractorService implements ApprovalHandler {

    private final ContractorRepository contractorRepository;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "CONTRACTOR"; }
    @Override public String getModuleCode()  { return "CONTRACTOR_MANAGEMENT"; }
    @Override public String getLabel(Long id) { return findOrThrow(id).getCompanyName(); }
    @Override public Object getSnapshot(Long id) { return ContractorResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            ContractorRequest req = objectMapper.readValue(json, ContractorRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<ContractorResponse> getAll() {
        return contractorRepository.findAllByDeletedFalse().stream()
                .map(ContractorResponse::from)
                .toList();
    }

    public ContractorResponse getById(Long id) {
        return ContractorResponse.from(findOrThrow(id));
    }

    @Transactional
    public ContractorResponse create(ContractorRequest request) {
        if (contractorRepository.existsByVoenAndDeletedFalse(request.getVoen())) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return ContractorResponse.from(contractorRepository.save(toEntity(request, new Contractor())));
    }

    @Transactional
    @RequiresApproval(module = "CONTRACTOR_MANAGEMENT", entityType = "CONTRACTOR")
    public ContractorResponse update(Long id, ContractorRequest request) {
        Contractor contractor = findOrThrow(id);
        if (contractorRepository.existsByVoenAndIdNotAndDeletedFalse(request.getVoen(), id)) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return ContractorResponse.from(contractorRepository.save(toEntity(request, contractor)));
    }

    @Transactional
    @RequiresApproval(module = "CONTRACTOR_MANAGEMENT", entityType = "CONTRACTOR", isDelete = true)
    public void delete(Long id) {
        Contractor contractor = findOrThrow(id);
        contractor.softDelete();
        contractorRepository.save(contractor);
    }

    private Contractor findOrThrow(Long id) {
        return contractorRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Podratçı", id));
    }

    private Contractor toEntity(ContractorRequest r, Contractor c) {
        c.setCompanyName(r.getCompanyName());
        c.setVoen(r.getVoen());
        c.setContactPerson(r.getContactPerson());
        c.setPhone(r.getPhone());
        c.setAddress(r.getAddress());
        c.setPaymentType(r.getPaymentType());
        c.setStatus(r.getStatus());
        c.setRating(r.getRating() != null ? r.getRating() : java.math.BigDecimal.ZERO);
        c.setRiskLevel(r.getRiskLevel());
        c.setNotes(r.getNotes());
        return c;
    }
}
