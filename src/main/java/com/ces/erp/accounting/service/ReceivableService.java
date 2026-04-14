package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.AddPaymentRequest;
import com.ces.erp.accounting.dto.ReceivablePaymentResponse;
import com.ces.erp.accounting.dto.ReceivableResponse;
import com.ces.erp.accounting.entity.Receivable;
import com.ces.erp.accounting.entity.ReceivablePayment;
import com.ces.erp.accounting.repository.ReceivablePaymentRepository;
import com.ces.erp.accounting.repository.ReceivableRepository;
import com.ces.erp.customer.entity.Customer;
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
    private final com.ces.erp.accounting.repository.InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> getReceivables(ReceivableStatus status, String search, Pageable pageable) {
        String safeSearch = (search == null || search.trim().isEmpty()) ? "" : search.trim();
        return receivableRepository.findAllWithFilters(status, safeSearch, pageable)
                .map(r -> {
                    List<com.ces.erp.accounting.entity.Invoice> invoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
                    return ReceivableResponse.from(r, invoices);
                });
    }

    @Transactional(readOnly = true)
    public ReceivableResponse getReceivable(Long id) {
        Receivable r = receivableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));
        List<com.ces.erp.accounting.entity.Invoice> invoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        return ReceivableResponse.from(r, invoices);
    }

    @Transactional
    public void createFromProject(Project project) {
        // Layihənin debitoru varmı?
        if (receivableRepository.findByProjectIdAndDeletedFalse(project.getId()).isPresent()) {
            return;
        }

        Customer customer = project.getRequest().getCustomer();
        LocalDate dueDate = project.getEndDate() != null ? project.getEndDate().plusDays(20) : LocalDate.now().plusDays(20);

        Receivable r = Receivable.builder()
                .project(project)
                .customer(customer)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .dueDate(dueDate)
                .status(ReceivableStatus.PENDING)
                .build();
        receivableRepository.save(r);
    }

    @Transactional
    public void syncInvoiceDebt(com.ces.erp.accounting.entity.Invoice invoice) {
        if (invoice == null || invoice.getProject() == null || invoice.getType() != com.ces.erp.enums.InvoiceType.INCOME) {
            return;
        }

        Receivable r = receivableRepository.findByProjectIdAndDeletedFalse(invoice.getProject().getId())
                .orElseGet(() -> {
                   createFromProject(invoice.getProject());
                   return receivableRepository.findByProjectIdAndDeletedFalse(invoice.getProject().getId()).get();
                });

        // Recalculate total amount from all project finalized income invoices
        BigDecimal totalAmount = invoiceRepository.findAllByProjectIdAndDeletedFalse(invoice.getProject().getId()).stream()
                .filter(i -> i.getType() == com.ces.erp.enums.InvoiceType.INCOME)
                .filter(i -> i.getInvoiceNumber() != null && !i.getInvoiceNumber().trim().isEmpty())
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        r.setTotalAmount(totalAmount);
        recalculateStatus(r);
    }

    @Transactional
    public ReceivablePaymentResponse addPayment(Long id, AddPaymentRequest req) {
        Receivable r = receivableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));

        if (req.getInvoiceId() == null) {
            throw new RuntimeException("Qaimə seçilməsi məcburidir");
        }

        com.ces.erp.accounting.entity.Invoice invoice = invoiceRepository.findByIdActive(req.getInvoiceId())
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
        List<com.ces.erp.accounting.entity.Invoice> invoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        return ReceivableResponse.from(r, invoices);
    }

    private void recalculateStatus(Receivable r) {
        List<ReceivablePayment> allPayments = receivablePaymentRepository.findAllByReceivableIdAndDeletedFalseOrderByPaymentDateAsc(r.getId());
        List<com.ces.erp.accounting.entity.Invoice> projectInvoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(r.getProject().getId());
        
        // Update per-invoice paid amount
        for (com.ces.erp.accounting.entity.Invoice inv : projectInvoices) {
            if (inv.getType() != com.ces.erp.enums.InvoiceType.INCOME) continue;
            
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
