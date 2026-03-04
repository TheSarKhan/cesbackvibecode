package com.ces.erp.investor.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.investor.dto.InvestorRequest;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestorService {

    private final InvestorRepository investorRepository;

    public List<InvestorResponse> getAll() {
        return investorRepository.findAllByDeletedFalse().stream()
                .map(InvestorResponse::from)
                .toList();
    }

    public InvestorResponse getById(Long id) {
        return InvestorResponse.from(findOrThrow(id));
    }

    @Transactional
    public InvestorResponse create(InvestorRequest request) {
        if (investorRepository.existsByVoenAndDeletedFalse(request.getVoen())) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return InvestorResponse.from(investorRepository.save(toEntity(request, new Investor())));
    }

    @Transactional
    public InvestorResponse update(Long id, InvestorRequest request) {
        Investor investor = findOrThrow(id);
        if (investorRepository.existsByVoenAndIdNotAndDeletedFalse(request.getVoen(), id)) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return InvestorResponse.from(investorRepository.save(toEntity(request, investor)));
    }

    @Transactional
    public void delete(Long id) {
        Investor investor = findOrThrow(id);
        investor.softDelete();
        investorRepository.save(investor);
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
