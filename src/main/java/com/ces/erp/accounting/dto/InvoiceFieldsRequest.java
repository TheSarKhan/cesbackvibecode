package com.ces.erp.accounting.dto;

import com.ces.erp.enums.InvoiceStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InvoiceFieldsRequest {

    private String invoiceNumber;       // Qaimə nömrəsi
    private String etaxesId;            // ETaxes ID
    private LocalDate invoiceDate;      // Tarix (düzəliş üçün)
    private String notes;               // Qeydlər
    private InvoiceStatus status;       // DRAFT, SENT (mühasibatlığa göndər/geri qaytar)
}
