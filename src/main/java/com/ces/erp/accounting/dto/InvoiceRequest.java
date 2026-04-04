package com.ces.erp.accounting.dto;

import com.ces.erp.enums.InvoiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceRequest {

    @NotNull(message = "Qaimə növü seçilməlidir")
    private InvoiceType type;

    private String invoiceNumber;

    @NotNull(message = "Məbləğ daxil edilməlidir")
    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal amount;

    @NotNull(message = "Tarix seçilməlidir")
    private LocalDate invoiceDate;

    private String etaxesId;        // Type A

    private String equipmentName;   // Type A, B1

    private String companyName;     // Type A (müştəri), B2 (xidmət şirkəti)

    private String serviceDescription; // Type B2

    private Long projectId;         // A, B1 məcburi; B2 könüllü

    private Long contractorId;      // B1 məcburi

    private String notes;

    // Aylıq iş cədvəli sahələri (INCOME növü, timesheet-əsaslı)
    private Integer periodMonth;
    private Integer periodYear;
    private Integer standardDays;
    private Integer extraDays;
    private BigDecimal extraHours;
    private BigDecimal monthlyRate;
    private Integer workingDaysInMonth;
    private Integer workingHoursPerDay;
    private BigDecimal overtimeRate;      // əlavə saat dərəcəsi (default 1.0)
}
