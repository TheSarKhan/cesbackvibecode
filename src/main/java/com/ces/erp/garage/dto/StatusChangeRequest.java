package com.ces.erp.garage.dto;

import com.ces.erp.enums.EquipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusChangeRequest {

    @NotNull(message = "Status boş ola bilməz")
    private EquipmentStatus status;

    private String reason;
}
