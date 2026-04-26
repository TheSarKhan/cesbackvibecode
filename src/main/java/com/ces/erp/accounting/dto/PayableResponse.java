package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.entity.Payable;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.PayableStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class PayableResponse {
    private Long id;
    private String projectCode;
    private String projectName;
    private String equipmentName;
    private String payeeName;     // contractor.companyName YOXSA investorName
    private String payeeVoen;     // contractor.voen YOXSA investorVoen
    private String ownershipType; // "CONTRACTOR" or "INVESTOR"

    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private PayableStatus status;
    private String notes;
    private List<InvoiceResponse> invoices;
    private List<PayablePaymentResponse> payments;

    public static PayableResponse from(Payable p) {
        return from(p, Collections.emptyList());
    }

    public static PayableResponse from(Payable p, List<Invoice> invoiceList) {
        String payeeName;
        String payeeVoen;
        String ownershipType;

        if (p.getContractor() != null) {
            payeeName = p.getContractor().getCompanyName();
            payeeVoen = p.getContractor().getVoen();
            ownershipType = "CONTRACTOR";
        } else {
            payeeName = p.getInvestorName();
            payeeVoen = p.getInvestorVoen();
            ownershipType = "INVESTOR";
        }

        String projectCode = null, projectName = null, equipmentName = null;
        if (p.getProject() != null) {
            projectCode = p.getProject().getProjectCode();
            if (p.getProject().getRequest() != null) {
                projectName = p.getProject().getRequest().getProjectName();
                // Koordinator planındakı texnikanı yoxla (request.selectedEquipment-dan fərqli ola bilər)
                if (p.getProject().getRequest().getSelectedEquipment() != null) {
                    equipmentName = p.getProject().getRequest().getSelectedEquipment().getName();
                }
            }
        }
        // Qaimədən texnika adını götür (daha etibarlı mənbə — autoCreateExpenseInvoice bunu doldurur)
        if (equipmentName == null && invoiceList != null) {
            equipmentName = invoiceList.stream()
                    .filter(i -> !i.isDeleted()
                            && (i.getType() == InvoiceType.CONTRACTOR_EXPENSE || i.getType() == InvoiceType.INVESTOR_EXPENSE)
                            && i.getEquipmentName() != null)
                    .map(Invoice::getEquipmentName)
                    .findFirst()
                    .orElse(null);
        }

        return PayableResponse.builder()
                .id(p.getId())
                .projectCode(projectCode)
                .projectName(projectName)
                .equipmentName(equipmentName)
                .payeeName(payeeName)
                .payeeVoen(payeeVoen)
                .ownershipType(ownershipType)
                .totalAmount(p.getTotalAmount())
                .paidAmount(p.getPaidAmount())
                .dueDate(p.getDueDate())
                .status(p.getStatus())
                .notes(p.getNotes())
                .invoices(invoiceList != null ? invoiceList.stream()
                        .filter(i -> !i.isDeleted()
                                && (i.getType() == InvoiceType.CONTRACTOR_EXPENSE || i.getType() == InvoiceType.INVESTOR_EXPENSE)
                                && i.getStatus() == InvoiceStatus.APPROVED
                                && i.getInvoiceNumber() != null && !i.getInvoiceNumber().trim().isEmpty())
                        .map(InvoiceResponse::from)
                        .toList() : Collections.emptyList())
                .payments(p.getPayments().stream()
                        .filter(pay -> !pay.isDeleted())
                        .map(PayablePaymentResponse::from)
                        .toList())
                .build();
    }
}
