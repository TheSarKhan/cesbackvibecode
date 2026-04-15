package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.Receivable;
import com.ces.erp.enums.ReceivableStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReceivableResponse {
    private Long id;
    private String projectCode;
    private String projectName;
    private String region;
    private String equipmentName;
    private String customerName;
    private String customerPhone;
    
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private ReceivableStatus status;
    private String notes;
    private List<InvoiceResponse> invoices;
    private List<ReceivablePaymentResponse> payments;

    public static ReceivableResponse from(Receivable r) {
        return from(r, java.util.Collections.emptyList());
    }

    public static ReceivableResponse from(Receivable r, List<com.ces.erp.accounting.entity.Invoice> invoices) {
        return ReceivableResponse.builder()
                .id(r.getId())
                .projectCode(r.getProject() != null ? r.getProject().getProjectCode() : null)
                .projectName(r.getProject() != null && r.getProject().getRequest() != null ? r.getProject().getRequest().getProjectName() : null)
                .region(r.getProject() != null && r.getProject().getRequest() != null ? r.getProject().getRequest().getRegion() : null)
                .equipmentName(r.getProject() != null && r.getProject().getRequest() != null && r.getProject().getRequest().getSelectedEquipment() != null ? r.getProject().getRequest().getSelectedEquipment().getName() : null)
                .customerName(r.getCustomer() != null ? r.getCustomer().getCompanyName() : null)
                .customerPhone(r.getCustomer() != null ? r.getCustomer().getOfficeContactPhone() : null)
                .totalAmount(r.getTotalAmount())
                .paidAmount(r.getPaidAmount())
                .dueDate(r.getDueDate())
                .status(r.getStatus())
                .notes(r.getNotes())
                .invoices(invoices != null ? invoices.stream()
                        .filter(i -> !i.isDeleted() && i.getType() == com.ces.erp.enums.InvoiceType.INCOME)
                        .filter(i -> i.getStatus() == com.ces.erp.enums.InvoiceStatus.APPROVED)
                        .map(InvoiceResponse::from)
                        .toList() : java.util.Collections.emptyList())
                .payments(r.getPayments().stream()
                        .filter(p -> !p.isDeleted())
                        .map(ReceivablePaymentResponse::from)
                        .toList())
                .build();
    }
}
