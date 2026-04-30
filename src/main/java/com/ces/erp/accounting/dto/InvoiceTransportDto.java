package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.InvoiceTransport;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceTransportDto {

    private Long id;

    @NotNull(message = "Daşınma tarixi daxil edilməlidir")
    private LocalDate transportDate;

    @NotNull(message = "Daşınma istiqaməti daxil edilməlidir")
    private String transportDirection;

    @NotNull(message = "Daşınma məbləği daxil edilməlidir")
    @DecimalMin(value = "0.01", message = "Daşınma məbləği 0-dan böyük olmalıdır")
    private BigDecimal transportAmount;

    public static InvoiceTransportDto from(InvoiceTransport t) {
        InvoiceTransportDto dto = new InvoiceTransportDto();
        dto.setId(t.getId());
        dto.setTransportDate(t.getTransportDate());
        dto.setTransportDirection(t.getTransportDirection());
        dto.setTransportAmount(t.getTransportAmount());
        return dto;
    }
}
