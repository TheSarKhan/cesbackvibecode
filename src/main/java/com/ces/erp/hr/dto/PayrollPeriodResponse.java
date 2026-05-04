package com.ces.erp.hr.dto;

import com.ces.erp.enums.PayrollStatus;
import com.ces.erp.hr.entity.PayrollPeriod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PayrollPeriodResponse {

    private Long id;
    private Integer year;
    private Integer month;
    private String label;
    private Integer workingDaysInMonth;
    private Integer workingHoursPerDay;
    private PayrollStatus status;

    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private BigDecimal totalEmployeeDeductions;
    private BigDecimal totalEmployerContributions;
    private BigDecimal totalIncomeTax;
    private Integer entryCount;

    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime paidAt;
    private Long invoiceId;
    private String notes;

    private List<PayrollEntryResponse> entries;

    private static final String[] AZ_MONTHS = {
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
        "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr"
    };

    public static PayrollPeriodResponse from(PayrollPeriod p, boolean withEntries) {
        String monthName = (p.getMonth() != null && p.getMonth() >= 1 && p.getMonth() <= 12)
                ? AZ_MONTHS[p.getMonth() - 1] : "";
        String label = monthName + " " + p.getYear();

        var b = PayrollPeriodResponse.builder()
                .id(p.getId())
                .year(p.getYear())
                .month(p.getMonth())
                .label(label)
                .workingDaysInMonth(p.getWorkingDaysInMonth())
                .workingHoursPerDay(p.getWorkingHoursPerDay())
                .status(p.getStatus())
                .totalGross(p.getTotalGross())
                .totalNet(p.getTotalNet())
                .totalEmployeeDeductions(p.getTotalEmployeeDeductions())
                .totalEmployerContributions(p.getTotalEmployerContributions())
                .totalIncomeTax(p.getTotalIncomeTax())
                .entryCount(p.getEntries() != null ? (int) p.getEntries().stream().filter(e -> !e.isDeleted()).count() : 0)
                .approvedAt(p.getApprovedAt())
                .approvedBy(p.getApprovedBy())
                .paidAt(p.getPaidAt())
                .invoiceId(p.getInvoiceId())
                .notes(p.getNotes());
        if (withEntries && p.getEntries() != null) {
            b.entries(p.getEntries().stream()
                    .filter(e -> !e.isDeleted())
                    .map(PayrollEntryResponse::from)
                    .toList());
        }
        return b.build();
    }
}
