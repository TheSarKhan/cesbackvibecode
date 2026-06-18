package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private InvoiceType type;
    private String typeLabel;
    private InvoiceStatus status;

    private String accountingId;        // Avtomatik: INV-2026-00001
    private String invoiceNumber;       // Əsl qaimə nömrəsi (könüllü)
    private BigDecimal amount;
    private LocalDate invoiceDate;

    // Debitor/Kreditor kontekstindən doldurulur
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    // Təhvil-Təslim Aktı
    private String aktFileName;
    private boolean aktFileUploaded;

    // Type A
    private String etaxesId;
    private String equipmentName;
    private Long equipmentId;       // Texnikaya ID bağlantısı (qazanc hesabatı üçün)
    private String equipmentCode;
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

    // Əlaqəli müştəri
    private Long customerId;
    private String customerName;

    // Maliyyə (Type A — layihənin xalis gəliri)
    private BigDecimal projectNetProfit;

    private Long sourceInvoiceId;

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

    // Texnika daşınması
    private boolean hasTransport;
    private List<InvoiceTransportDto> transports;
    private BigDecimal totalTransportAmount;

    // Toplu qaimə — texnika sətirləri (boşdursa köhnə tək-texnikalı qaimə)
    private List<InvoiceLineDto> lines;
    private Integer equipmentCount;   // neçə texnika (siyahıda "N texnika" üçün)

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

        List<InvoiceTransportDto> transportDtos = inv.getTransports().stream()
                .filter(t -> !t.isDeleted())
                .map(InvoiceTransportDto::from)
                .toList();

        BigDecimal totalTransport = transportDtos.stream()
                .map(InvoiceTransportDto::getTransportAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InvoiceLineDto> lineDtos = inv.getLines() == null ? List.of() : inv.getLines().stream()
                .filter(l -> !l.isDeleted())
                .map(InvoiceLineDto::from)
                .toList();

        // customer_id FK boş ola bilər (layihə qaimələrində) — project→request→customer zəncirinə fallback
        var effectiveCustomer = inv.getCustomer();
        if (effectiveCustomer == null && inv.getProject() != null
                && inv.getProject().getRequest() != null) {
            effectiveCustomer = inv.getProject().getRequest().getCustomer();
        }

        return InvoiceResponse.builder()
                .id(inv.getId())
                .type(inv.getType())
                .typeLabel(typeLabel)
                .status(inv.getStatus())
                .accountingId(inv.getAccountingId())
                .invoiceNumber(inv.getInvoiceNumber())
                .amount(inv.getAmount())
                .paidAmount(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO)
                .remainingAmount(
                        (inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO)
                                .subtract(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO))
                .invoiceDate(inv.getInvoiceDate())
                .etaxesId(inv.getEtaxesId())
                .equipmentName(inv.getEquipmentName())
                .equipmentId(inv.getEquipment() != null ? inv.getEquipment().getId() : null)
                .equipmentCode(inv.getEquipment() != null ? inv.getEquipment().getEquipmentCode() : null)
                .companyName(inv.getCompanyName())
                .serviceDescription(inv.getServiceDescription())
                .projectId(projectId)
                .projectCode(projectCode)
                .projectCompanyName(projectCompanyName)
                .projectName(projectName)
                .contractorId(inv.getContractor() != null ? inv.getContractor().getId() : null)
                .contractorName(inv.getContractor() != null ? inv.getContractor().getCompanyName() : null)
                .contractorVoen(inv.getContractor() != null ? inv.getContractor().getVoen() : null)
                .customerId(effectiveCustomer != null ? effectiveCustomer.getId() : null)
                .customerName(effectiveCustomer != null ? effectiveCustomer.getCompanyName() : null)
                .sourceInvoiceId(inv.getSourceInvoiceId())
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
                .hasTransport(inv.isHasTransport())
                .transports(transportDtos)
                .totalTransportAmount(totalTransport)
                .lines(lineDtos)
                .equipmentCount(lineDtos.isEmpty() ? (inv.getEquipment() != null ? 1 : 0) : lineDtos.size())
                .aktFileName(inv.getAktFileName())
                .aktFileUploaded(inv.getAktFilePath() != null)
                .build();
    }
}
