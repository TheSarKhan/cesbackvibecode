package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.Receivable;
import com.ces.erp.enums.ReceivableStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class ReceivableResponse {
    private Long id;
    private String projectCode;
    private String projectName;
    private String region;
    private String equipmentName;
    private String customerName;
    private String customerVoen;
    private String customerPhone;
    
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private ReceivableStatus status;
    private String notes;
    private List<InvoiceResponse> invoices;
    private List<ReceivablePaymentResponse> payments;

    private static List<InvoiceResponse> buildInvoiceLines(
            List<com.ces.erp.accounting.entity.Invoice> invoices, Receivable r) {
        Map<Long, BigDecimal> paidByInvoice = r.getPayments().stream()
                .filter(p -> !p.isDeleted() && p.getInvoice() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getInvoice().getId(),
                        Collectors.reducing(BigDecimal.ZERO, p -> p.getAmount(), BigDecimal::add)));
        return invoices.stream()
                .filter(i -> !i.isDeleted() && i.getType() == com.ces.erp.enums.InvoiceType.INCOME)
                .filter(i -> i.getStatus() == com.ces.erp.enums.InvoiceStatus.APPROVED)
                .map(inv -> {
                    InvoiceResponse ir = InvoiceResponse.from(inv);
                    BigDecimal paid = paidByInvoice.getOrDefault(inv.getId(), BigDecimal.ZERO);
                    ir.setPaidAmount(paid);
                    ir.setRemainingAmount(inv.getAmount() != null ? inv.getAmount().subtract(paid) : BigDecimal.ZERO);
                    return ir;
                })
                .toList();
    }

    public static ReceivableResponse from(Receivable r) {
        return from(r, java.util.Collections.emptyList());
    }

    public static ReceivableResponse from(Receivable r, List<com.ces.erp.accounting.entity.Invoice> invoices) {
        // Texnika adı — gəlir qaimələrindəki texnikalar (çoxlu ola bilər)
        String equipmentName = null;
        if (invoices != null) {
            List<String> names = invoices.stream()
                    .filter(i -> !i.isDeleted() && i.getType() == com.ces.erp.enums.InvoiceType.INCOME && i.getEquipmentName() != null)
                    .map(com.ces.erp.accounting.entity.Invoice::getEquipmentName)
                    .distinct()
                    .toList();
            if (!names.isEmpty()) {
                equipmentName = names.size() <= 2 ? String.join(", ", names) : (names.size() + " texnika");
            }
        }
        if (equipmentName == null && r.getProject() != null && r.getProject().getRequest() != null
                && r.getProject().getRequest().getSelectedEquipment() != null) {
            equipmentName = r.getProject().getRequest().getSelectedEquipment().getName();
        }
        // Müştəri FK yoxdursa — layihənin companyName mətnini göstər
        String customerName = r.getCustomer() != null ? r.getCustomer().getCompanyName()
                : (r.getProject() != null && r.getProject().getRequest() != null ? r.getProject().getRequest().getCompanyName() : null);
        return ReceivableResponse.builder()
                .id(r.getId())
                .projectCode(r.getProject() != null ? r.getProject().getProjectCode() : null)
                .projectName(r.getProject() != null && r.getProject().getRequest() != null ? r.getProject().getRequest().getProjectName() : null)
                .region(r.getProject() != null && r.getProject().getRequest() != null ? r.getProject().getRequest().getRegion() : null)
                .equipmentName(equipmentName)
                .customerName(customerName)
                .customerVoen(r.getCustomer() != null ? r.getCustomer().getVoen() : null)
                .customerPhone(r.getCustomer() != null ? r.getCustomer().getOfficeContactPhone() : null)
                .totalAmount(r.getTotalAmount())
                .paidAmount(r.getPaidAmount())
                .dueDate(r.getDueDate())
                .status(r.getStatus())
                .notes(r.getNotes())
                .invoices(invoices != null ? buildInvoiceLines(invoices, r) : java.util.Collections.emptyList())
                .payments(r.getPayments().stream()
                        .filter(p -> !p.isDeleted())
                        .map(ReceivablePaymentResponse::from)
                        .toList())
                .build();
    }
}
