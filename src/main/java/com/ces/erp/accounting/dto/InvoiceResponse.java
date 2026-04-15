package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private InvoiceType type;
    private String typeLabel;
    private InvoiceStatus status;

    private String invoiceNumber;
    private BigDecimal amount;
    private LocalDate invoiceDate;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    // Type A
    private String etaxesId;
    private String equipmentName;
    private String companyName;

    // Type B2
    private String serviceDescription;

    // Əlaqəli layihə
    private Long projectId;
    private String projectCode;
    private String projectCompanyName;
    private String projectName;

    // Əlaqəli podratçı (B1)
    private Long contractorId;
    private String contractorName;
    private String contractorVoen;

    // Əlaqəli investor (B1)
    private Long investorId;
    private String investorName;
    private String investorVoen;

    // Maliyyə (Type A — layihənin xalis gəliri)
    private BigDecimal projectNetProfit;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Aylıq iş cədvəli
    private Integer periodMonth;
    private Integer periodYear;
    private Integer standardDays;
    private Integer extraDays;
    private BigDecimal extraHours;
    private BigDecimal monthlyRate;
    private Integer workingDaysInMonth;
    private Integer workingHoursPerDay;
    private BigDecimal overtimeRate;

    public static InvoiceResponse from(Invoice inv) {
        String typeLabel = switch (inv.getType()) {
            case INCOME             -> "Gəlir";
            case CONTRACTOR_EXPENSE -> "Ödəmə";
            case COMPANY_EXPENSE    -> "Xərc";
            case INVESTOR_EXPENSE   -> "İnvestor Ödəməsi";
        };

        BigDecimal netProfit = null;
        String projectCode = null;
        String projectCompanyName = null;
        String projectName = null;
        Long projectId = null;

        if (inv.getProject() != null) {
            projectId = inv.getProject().getId();
            projectCode = inv.getProject().getProjectCode();
            var request = inv.getProject().getRequest();
            if (request != null) {
                projectCompanyName = request.getCompanyName();
                projectName = request.getProjectName();
            }
            if (inv.getType() == InvoiceType.INCOME) {
                BigDecimal totalRev = inv.getProject().getRevenues().stream()
                        .filter(r -> !r.isDeleted())
                        .map(r -> r.getValue())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalExp = inv.getProject().getExpenses().stream()
                        .filter(e -> !e.isDeleted())
                        .map(e -> e.getValue())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                netProfit = totalRev.subtract(totalExp);
            }
        }

        return InvoiceResponse.builder()
                .id(inv.getId())
                .type(inv.getType())
                .typeLabel(typeLabel)
                .status(inv.getStatus())
                .invoiceNumber(inv.getInvoiceNumber())
                .amount(inv.getAmount())
                .invoiceDate(inv.getInvoiceDate())
                .etaxesId(inv.getEtaxesId())
                .equipmentName(inv.getEquipmentName())
                .companyName(inv.getCompanyName())
                .serviceDescription(inv.getServiceDescription())
                .projectId(projectId)
                .projectCode(projectCode)
                .projectCompanyName(projectCompanyName)
                .projectName(projectName)
                .contractorId(inv.getContractor() != null ? inv.getContractor().getId() : null)
                .contractorName(inv.getContractor() != null ? inv.getContractor().getCompanyName() : null)
                .contractorVoen(inv.getContractor() != null ? inv.getContractor().getVoen() : null)
                .investorId(inv.getInvestor() != null ? inv.getInvestor().getId() : null)
                .investorName(inv.getInvestor() != null ? inv.getInvestor().getCompanyName() : null)
                .investorVoen(inv.getInvestor() != null ? inv.getInvestor().getVoen() : null)
                .projectNetProfit(netProfit)
                .notes(inv.getNotes())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .periodMonth(inv.getPeriodMonth())
                .periodYear(inv.getPeriodYear())
                .standardDays(inv.getStandardDays())
                .extraDays(inv.getExtraDays())
                .extraHours(inv.getExtraHours())
                .monthlyRate(inv.getMonthlyRate())
                .workingDaysInMonth(inv.getWorkingDaysInMonth())
                .workingHoursPerDay(inv.getWorkingHoursPerDay())
                .overtimeRate(inv.getOvertimeRate())
                .paidAmount(inv.getPaidAmount())
                .remainingAmount(inv.getAmount() != null ? inv.getAmount().subtract(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO) : BigDecimal.ZERO)
                .build();
    }
}
