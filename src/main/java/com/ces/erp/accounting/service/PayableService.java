package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.AddPayablePaymentRequest;
import com.ces.erp.accounting.dto.PayablePaymentResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.entity.Payable;
import com.ces.erp.accounting.entity.PayablePayment;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.accounting.repository.PayablePaymentRepository;
import com.ces.erp.accounting.repository.PayableRepository;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.PayableStatus;
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
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayablePaymentRepository payablePaymentRepository;
    private final InvoiceRepository invoiceRepository;

    // ─── Sync / Create ────────────────────────────────────────────────────────

    /**
     * InvoiceService.approve() tərəfindən çağırılır.
     * CONTRACTOR_EXPENSE və ya INVESTOR_EXPENSE qaiməsi təsdiqlənəndə Payable yaradır/yeniləyir.
     */
    @Transactional
    public void syncPayableDebt(Invoice invoice) {
        if (invoice == null || invoice.getProject() == null) return;
        if (invoice.getType() != InvoiceType.CONTRACTOR_EXPENSE
                && invoice.getType() != InvoiceType.INVESTOR_EXPENSE) return;

        var project = invoice.getProject();

        Payable payable = payableRepository.findByProjectIdAndDeletedFalse(project.getId())
                .orElse(null);

        if (payable == null) {
            // Yeni Payable yarat
            LocalDate dueDate = project.getEndDate() != null
                    ? project.getEndDate().plusDays(30)
                    : LocalDate.now().plusDays(30);

            Payable.PayableBuilder builder = Payable.builder()
                    .project(project)
                    .totalAmount(invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(dueDate)
                    .status(PayableStatus.PENDING);

            if (invoice.getType() == InvoiceType.CONTRACTOR_EXPENSE && invoice.getContractor() != null) {
                builder.contractor(invoice.getContractor());
            } else {
                // INVESTOR_EXPENSE — companyName sahəsindən investor adını oxu
                builder.investorName(invoice.getCompanyName());
                builder.investorVoen(null);
            }

            payableRepository.save(builder.build());
        } else {
            // Mövcud Payable-ı yenilə
            payable.setTotalAmount(invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO);
            recalculateStatus(payable);
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PayableResponse> getPayables(PayableStatus status, String search, Pageable pageable) {
        String safeSearch = (search == null || search.trim().isEmpty()) ? "" : search.trim();
        return payableRepository.findAllWithFilters(status, safeSearch, pageable)
                .map(p -> {
                    List<Invoice> invoices = p.getProject() != null
                            ? invoiceRepository.findAllByProjectIdAndDeletedFalse(p.getProject().getId())
                            : List.of();
                    return PayableResponse.from(p, invoices);
                });
    }

    @Transactional(readOnly = true)
    public PayableResponse getPayable(Long id) {
        Payable p = payableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Payable tapılmadı"));
        List<Invoice> invoices = p.getProject() != null
                ? invoiceRepository.findAllByProjectIdAndDeletedFalse(p.getProject().getId())
                : List.of();
        return PayableResponse.from(p, invoices);
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    @Transactional
    public PayablePaymentResponse addPayment(Long id, AddPayablePaymentRequest req) {
        Payable p = payableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Payable tapılmadı"));

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Məbləğ düzgün deyil");
        }

        // Qaimə seçimi (məcburi)
        Invoice invoice = null;
        if (req.getInvoiceId() == null) {
            throw new RuntimeException("Qaimə seçilməsi məcburidir");
        }
        invoice = invoiceRepository.findByIdActive(req.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Qaimə tapılmadı"));
        if (p.getProject() == null || !invoice.getProject().getId().equals(p.getProject().getId())) {
            throw new RuntimeException("Bu qaimə başqa bir layihəyə aiddir");
        }
        // Qaimə üzrə ödəniş limitini yoxla
        BigDecimal invPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        if (invPaid.add(req.getAmount()).compareTo(invoice.getAmount()) > 0) {
            BigDecimal remaining = invoice.getAmount().subtract(invPaid);
            throw new RuntimeException("Ödəniş qaimə məbləğini keçə bilməz. Maksimum: " + remaining + " ₼");
        }

        PayablePayment payment = PayablePayment.builder()
                .payable(p)
                .invoice(invoice)
                .amount(req.getAmount())
                .paymentDate(req.getPaymentDate())
                .note(req.getNote())
                .build();
        payablePaymentRepository.save(payment);

        recalculateStatus(p);

        return PayablePaymentResponse.from(payment);
    }

    @Transactional
    public void deletePayment(Long id, Long paymentId) {
        Payable p = payableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Payable tapılmadı"));

        PayablePayment payment = payablePaymentRepository.findByIdAndPayableIdAndDeletedFalse(paymentId, id)
                .orElseThrow(() -> new RuntimeException("Ödəniş tapılmadı"));

        payment.setDeleted(true);
        payablePaymentRepository.save(payment);

        recalculateStatus(p);
    }

    @Transactional
    public PayableResponse complete(Long id) {
        Payable p = payableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Payable tapılmadı"));

        p.setStatus(PayableStatus.COMPLETED);
        payableRepository.save(p);
        List<Invoice> invoices = p.getProject() != null
                ? invoiceRepository.findAllByProjectIdAndDeletedFalse(p.getProject().getId())
                : List.of();
        return PayableResponse.from(p, invoices);
    }

    // ─── Status hesablanması ──────────────────────────────────────────────────

    private void recalculateStatus(Payable p) {
        List<PayablePayment> allPayments =
                payablePaymentRepository.findAllByPayableIdAndDeletedFalseOrderByPaymentDateAsc(p.getId());

        BigDecimal paid = allPayments.stream()
                .map(PayablePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        p.setPaidAmount(paid);

        // Hər qaimənin paidAmount-unu yenilə
        if (p.getProject() != null) {
            List<Invoice> projectInvoices = invoiceRepository.findAllByProjectIdAndDeletedFalse(p.getProject().getId());
            for (Invoice inv : projectInvoices) {
                if (inv.getType() != InvoiceType.CONTRACTOR_EXPENSE && inv.getType() != InvoiceType.INVESTOR_EXPENSE) continue;
                BigDecimal invPaid = allPayments.stream()
                        .filter(pay -> pay.getInvoice() != null && pay.getInvoice().getId().equals(inv.getId()))
                        .map(PayablePayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                inv.setPaidAmount(invPaid);
                invoiceRepository.save(inv);
            }
        }

        if (p.getStatus() != PayableStatus.COMPLETED) {
            if (paid.compareTo(p.getTotalAmount()) >= 0 && p.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                p.setStatus(PayableStatus.COMPLETED);
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                if (p.getDueDate() != null && p.getDueDate().isBefore(LocalDate.now())) {
                    p.setStatus(PayableStatus.OVERDUE);
                } else {
                    p.setStatus(PayableStatus.PARTIAL);
                }
            } else {
                p.setStatus(p.getDueDate() != null && p.getDueDate().isBefore(LocalDate.now())
                        ? PayableStatus.OVERDUE : PayableStatus.PENDING);
            }
        }

        payableRepository.save(p);
    }

    // Hər gün səhər 08:00 — gecikmiş payable-ları OVERDUE et
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkOverduePayables() {
        LocalDate today = LocalDate.now();
        List<Payable> overdueList = payableRepository.findAllByDueDateBeforeAndStatusInAndDeletedFalse(
                today, List.of(PayableStatus.PENDING, PayableStatus.PARTIAL));

        for (Payable p : overdueList) {
            p.setStatus(PayableStatus.OVERDUE);
            payableRepository.save(p);
        }
    }
}
