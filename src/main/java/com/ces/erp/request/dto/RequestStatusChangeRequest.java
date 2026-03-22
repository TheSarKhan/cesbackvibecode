package com.ces.erp.request.dto;

import com.ces.erp.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestStatusChangeRequest {

    @NotNull(message = "Status tələb olunur")
    private RequestStatus status;

    private String reason;
}
