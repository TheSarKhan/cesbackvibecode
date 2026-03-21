package com.ces.erp.approval.service;

import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.approval.repository.PendingOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalPersistenceService {

    private final PendingOperationRepository repository;

    /**
     * REQUIRES_NEW: kənar transaksiya rollback olsa belə bu commit olur.
     * DTO-nu transaksiya içərisində qururuq ki lazy-load problemi olmasın.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PendingOperationResponse saveAndBuildResponse(PendingOperation op) {
        PendingOperation saved = repository.save(op);
        return PendingOperationResponse.from(saved);
    }
}
