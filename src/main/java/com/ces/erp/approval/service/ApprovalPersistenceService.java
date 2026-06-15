package com.ces.erp.approval.service;

import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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

    /**
     * Eyni entity üçün mövcud PENDING op varsa response kimi qaytarır.
     * Transaksiya içində lazy əlaqələr load olunur.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<PendingOperationResponse> findExistingPending(String entityType, Long entityId) {
        return repository
                .findFirstByEntityTypeAndEntityIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
                        entityType, entityId, OperationStatus.PENDING)
                .map(op -> {
                    // Lazy fields-ı triggerle
                    if (op.getPerformedBy() != null) op.getPerformedBy().getFullName();
                    if (op.getPerformerDepartment() != null) op.getPerformerDepartment().getName();
                    return PendingOperationResponse.from(op);
                });
    }

    /**
     * Mövcud PENDING EDIT-i avtomatik rədd edir (DELETE üst-üstə düşəndə).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoRejectExistingOperation(Long opId, User performer, String reason) {
        repository.findById(opId).ifPresent(op -> {
            if (op.getStatus() != OperationStatus.PENDING) return;
            op.setStatus(OperationStatus.REJECTED);
            op.setProcessedBy(performer);
            op.setProcessedAt(LocalDateTime.now());
            op.setRejectReason(reason);
            repository.save(op);
        });
    }
}
