package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.AddPaymentRequest;
import com.ces.erp.accounting.dto.ReceivablePaymentResponse;
import com.ces.erp.accounting.dto.ReceivableResponse;
import com.ces.erp.accounting.entity.Receivable;
import com.ces.erp.accounting.entity.ReceivablePayment;
import com.ces.erp.accounting.repository.ReceivablePaymentRepository;
import com.ces.erp.accounting.repository.ReceivableRepository;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.ReceivableStatus;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.entity.ProjectRevenue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final ReceivablePaymentRepository receivablePaymentRepository;
    private final com.ces.erp.accounting.repository.InvoiceRepository invoiceRepository; // NOSONAR — InvoiceRepository import conflict avoided
    private final CoordinatorPlanRepository coordinatorPlanRepository;

    @Transactional
    public Page<ReceivableResponse> getReceivables(ReceivableStatus status, String search, Pageable pageable) {
        String safeSearch = (search == null || search.trim().isEmpty()) ? "" : search.trim();
        return receivableRepository.findAllWithFilters(status, safeSearch, pageable)
                .map(r -> {
                    syncTotalFromInvoices(r);
                    List<Invoice> invoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
                    return ReceivableResponse.from(r, invoices);
                });
    }

    @Transactional
    public ReceivableResponse getReceivable(Long id) {
        Receivable r = receivableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));
        syncTotalFromInvoices(r);
        List<Invoice> invoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        return ReceivableResponse.from(r, invoices);
    }

    /**
     * totalAmount-u həmişə APPROVED INCOME qaimələrin cəminə əsasən hesabla.
     * Əgər heç bir APPROVED qaimə yoxdursa — planın dəyərini istifadə et (fallback).
     */
    private void syncTotalFromInvoices(Receivable r) {
        if (r.getProject() == null) return;
        List<Invoice> allInvoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        BigDecimal invoiceTotal = allInvoices.stream()
                .filter(i -> i.getType() == InvoiceType.INCOME && i.getStatus() == InvoiceStatus.APPROVED)
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (invoiceTotal.compareTo(BigDecimal.ZERO) > 0) {
            if (!invoiceTotal.equals(r.getTotalAmount())) {
                r.setTotalAmount(invoiceTotal);
                receivableRepository.save(r);
            }
            return;
        }
        // Fallback: heç bir APPROVED qaimə yoxdursa — planın dəyərini götür
        if (r.getTotalAmount() != null && r.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) return;
        if (r.getProject().getRequest() == null) return;
        CoordinatorPlan plan = coordinatorPlanRepository.findByRequestId(r.getProject().getRequest().getId()).orElse(null);
        if (plan == null) return;
        BigDecimal eqPrice = plan.getEquipmentPrice() != null ? plan.getEquipmentPrice() : BigDecimal.ZERO;
        BigDecimal transPrice = plan.getTransportationPrice() != null ? plan.getTransportationPrice() : BigDecimal.ZERO;
        int days = plan.getDayCount() != null ? plan.getDayCount() : 0;
        ProjectType type = r.getProject().getRequest().getProjectType();
        BigDecimal eqTotal = (type == ProjectType.MONTHLY || days == 0)
                ? eqPrice : eqPrice.multiply(BigDecimal.valueOf(days));
        BigDecimal computed = eqTotal.add(transPrice);
        if (computed.compareTo(BigDecimal.ZERO) > 0) {
            r.setTotalAmount(computed);
            receivableRepository.save(r);
        }
    }

    @Transactional
    public void createFromProject(Project project) {
        // Layihənin debitoru varmı?
        if (receivableRepository.findByProjectIdAndDeletedFalse(project.getId()).isPresent()) {
            return;
        }

        Customer customer = project.getRequest().getCustomer();
        LocalDate dueDate = project.getEndDate() != null ? project.getEndDate().plusDays(20) : LocalDate.now().plusDays(20);

        // Koordinator planından gözlənilən ümumi məbləği hesabla
        BigDecimal initialTotal = BigDecimal.ZERO;
        if (project.getRequest() != null) {
            CoordinatorPlan plan = coordinatorPlanRepository.findByRequestId(project.getRequest().getId()).orElse(null);
            if (plan != null) {
                BigDecimal eqPrice = plan.getEquipmentPrice() != null ? plan.getEquipmentPrice() : BigDecimal.ZERO;
                BigDecimal transPrice = plan.getTransportationPrice() != null ? plan.getTransportationPrice() : BigDecimal.ZERO;
                int days = plan.getDayCount() != null ? plan.getDayCount() : 0;
                ProjectType type = project.getRequest().getProjectType();
                BigDecimal eqTotal = (type == ProjectType.MONTHLY || days == 0)
                        ? eqPrice
                        : eqPrice.multiply(BigDecimal.valueOf(days));
                initialTotal = eqTotal.add(transPrice);
            }
        }

        Receivable r = Receivable.builder()
                .project(project)
                .customer(customer)
                .totalAmount(initialTotal)
                .paidAmount(BigDecimal.ZERO)
                .dueDate(dueDate)
                .status(ReceivableStatus.PENDING)
                .build();
        receivableRepository.save(r);
    }

    @Transactional
    public void syncInvoiceDebt(Invoice invoice) {
        if (invoice == null || invoice.getProject() == null || invoice.getType() != InvoiceType.INCOME) {
            return;
        }

        Receivable r = receivableRepository.findByProjectIdAndDeletedFalse(invoice.getProject().getId())
                .orElseGet(() -> {
                   createFromProject(invoice.getProject());
                   return receivableRepository.findByProjectIdAndDeletedFalse(invoice.getProject().getId()).get();
                });

        // Yalnız APPROVED INCOME qaimələrinin cəmi — borc məbləği
        BigDecimal totalAmount = invoiceRepository.findAllByProjectIdAndDeletedFalse(invoice.getProject().getId()).stream()
                .filter(i -> i.getType() == InvoiceType.INCOME && i.getStatus() == InvoiceStatus.APPROVED)
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            r.setTotalAmount(totalAmount);
        }
        recalculateStatus(r);
    }

    @Transactional
    public ReceivablePaymentResponse addPayment(Long id, AddPaymentRequest req) {
        Receivable r = receivableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));

        if (req.getInvoiceId() == null) {
            throw new RuntimeException("Qaimə seçilməsi məcburidir");
        }

        Invoice invoice = invoiceRepository.findByIdActive(req.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (!invoice.getProject().getId().equals(r.getProject().getId())) {
             throw new RuntimeException("Bu qaimə başqa bir layihəyə aiddir");
        }

        // Validation: Cannot pay more than invoice amount
        BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newAmount = req.getAmount();
        if (currentPaid.add(newAmount).compareTo(invoice.getAmount()) > 0) {
            BigDecimal remaining = invoice.getAmount().subtract(currentPaid);
            throw new RuntimeException("Ödəniş qaimə məbləğini keçə bilməz. Maksimum ödənilə bilən: " + remaining + " ₼");
        }

        ReceivablePayment p = ReceivablePayment.builder()
                .receivable(r)
                .invoice(invoice)
                .amount(req.getAmount())
                .paymentDate(req.getPaymentDate())
                .note(req.getNote())
                .build();
        receivablePaymentRepository.save(p);

        recalculateStatus(r);

        return ReceivablePaymentResponse.from(p);
    }

    @Transactional
    public void deletePayment(Long id, Long paymentId) {
        Receivable r = receivableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));

        ReceivablePayment p = receivablePaymentRepository.findByIdAndReceivableIdAndDeletedFalse(paymentId, id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        p.setDeleted(true);
        receivablePaymentRepository.save(p);

        recalculateStatus(r);
    }

    @Transactional
    public ReceivableResponse complete(Long id) {
        Receivable r = receivableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));

        r.setStatus(ReceivableStatus.COMPLETED);
        receivableRepository.save(r);
        List<Invoice> invoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        return ReceivableResponse.from(r, invoices);
    }

    private void recalculateStatus(Receivable r) {
        List<ReceivablePayment> allPayments = receivablePaymentRepository.findAllByReceivableIdAndDeletedFalseOrderByPaymentDateAsc(r.getId());
        List<Invoice> projectInvoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        
        // Update per-invoice paid amount
        for (Invoice inv : projectInvoices) {
            if (inv.getType() != InvoiceType.INCOME) continue;
            
            BigDecimal invPaid = allPayments.stream()
                    .filter(p -> p.getInvoice() != null && p.getInvoice().getId().equals(inv.getId()))
                    .map(ReceivablePayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            inv.setPaidAmount(invPaid);
        }

        BigDecimal paid = allPayments.stream()
                .map(ReceivablePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        r.setPaidAmount(paid);

        if (r.getStatus() != ReceivableStatus.COMPLETED) {
            if (paid.compareTo(r.getTotalAmount()) >= 0 && r.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                r.setStatus(ReceivableStatus.COMPLETED);
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                r.setStatus(ReceivableStatus.PARTIAL);
                if(r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now())){
                    r.setStatus(ReceivableStatus.OVERDUE);
                }
            } else {
                r.setStatus(r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now()) ? ReceivableStatus.OVERDUE : ReceivableStatus.PENDING);
            }
        }

        receivableRepository.save(r);
    }

    // Hər gün səhər 08:00
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkOverdueReceivables() {
        LocalDate today = LocalDate.now();
        List<Receivable> overdueList = receivableRepository.findAllByDueDateBeforeAndStatusInAndDeletedFalse(
                today, List.of(ReceivableStatus.PENDING, ReceivableStatus.PARTIAL));

        for (Receivable r : overdueList) {
            r.setStatus(ReceivableStatus.OVERDUE);
            receivableRepository.save(r);
        }
    }
}
