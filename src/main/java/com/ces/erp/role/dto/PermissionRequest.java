package com.ces.erp.role.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionRequest {

    @NotNull(message = "Modul ID boş ola bilməz")
    private Long moduleId;

    private boolean canGet;
    private boolean canPost;
    private boolean canPut;
    private boolean canDelete;
    private boolean canSendToCoordinator;
    private boolean canSubmitOffer;
    private boolean canSendToAccounting;
    private boolean canReturnToProject;
    private boolean canGetCustomer;
}
