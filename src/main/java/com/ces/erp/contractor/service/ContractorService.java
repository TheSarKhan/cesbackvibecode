package com.ces.erp.contractor.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.contractor.dto.ContractorRequest;
import com.ces.erp.contractor.dto.ContractorResponse;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractorService {

    private final ContractorRepository contractorRepository;

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
    public ContractorResponse update(Long id, ContractorRequest request) {
        Contractor contractor = findOrThrow(id);
        if (contractorRepository.existsByVoenAndIdNotAndDeletedFalse(request.getVoen(), id)) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return ContractorResponse.from(contractorRepository.save(toEntity(request, contractor)));
    }

    @Transactional
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
        c.setAddress(r.getAddress());
        c.setPaymentType(r.getPaymentType());
        c.setStatus(r.getStatus());
        c.setRating(r.getRating() != null ? r.getRating() : java.math.BigDecimal.ZERO);
        c.setRiskLevel(r.getRiskLevel());
        c.setNotes(r.getNotes());
        return c;
    }
}
