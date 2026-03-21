package com.ces.erp.approval.exception;

import com.ces.erp.approval.dto.PendingOperationResponse;

public class PendingApprovalException extends RuntimeException {

    private final PendingOperationResponse operation;

    public PendingApprovalException(PendingOperationResponse operation) {
        super("Əməliyyat təsdiq gözləyir");
        this.operation = operation;
    }

    public PendingOperationResponse getOperation() {
        return operation;
    }
}
