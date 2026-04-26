package com.ces.erp.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DocumentLineRequest {

    @NotBlank(message = "Təsvir boş ola bilməz")
    private String description;

    @NotBlank(message = "Vahid boş ola bilməz")
    private String unit;

    @NotNull(message = "Miqdar mütləqdir")
    private BigDecimal quantity;

    @NotNull(message = "Vahid qiymət mütləqdir")
    private BigDecimal unitPrice;

    /** Traceability: hansı qaimədən yarandı */
    private Long sourceInvoiceId;
}
