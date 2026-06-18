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
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.PayableStatus;
import com.ces.erp.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayablePaymentRepository payablePaymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final com.ces.erp.investor.service.PortalNotificationService portalNotificationService;

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
        recomputeProjectPayables(invoice.getProject());
    }

    /**
     * Çoxlu sahib: layihənin BÜTÜN təsdiqlənmiş xərc qaimələrini sahibə görə qruplaşdırır
     * və hər sahib (podratçı/investor) üçün ayrı Payable saxlayır (məbləğ = həmin sahibin
     * qaimələrinin cəmi). Beləliklə müxtəlif sahiblərin borcu itmir / üst-üstə yazılmır.
     */
    private void recomputeProjectPayables(Project project) {
        List<Invoice> expenses = invoiceRepository.findAllByProjectIdAndDeletedFalse(project.getId()).stream()
                .filter(i -> (i.getType() == InvoiceType.CONTRACTOR_EXPENSE || i.getType() == InvoiceType.INVESTOR_EXPENSE)
                        && i.getStatus() == InvoiceStatus.APPROVED)
                .toList();

        Map<String, List<Invoice>> byOwner = new LinkedHashMap<>();
        for (Invoice i : expenses) {
            byOwner.computeIfAbsent(ownerKey(i), k -> new ArrayList<>()).add(i);
        }

        List<Payable> existing = payableRepository.findAllByProjectIdAndDeletedFalse(project.getId());
        LocalDate dueDate = project.getEndDate() != null
                ? project.getEndDate().plusDays(30) : LocalDate.now().plusDays(30);

        for (var entry : byOwner.entrySet()) {
            List<Invoice> grp = entry.getValue();
            Invoice rep = grp.get(0);
            BigDecimal total = grp.stream()
                    .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Payable payable = existing.stream()
                    .filter(p -> ownerKey(p).equals(entry.getKey()))
                    .findFirst().orElse(null);

            if (payable == null) {
                Payable.PayableBuilder builder = Payable.builder()
                        .project(project)
                        .totalAmount(total)
                        .paidAmount(BigDecimal.ZERO)
                        .dueDate(dueDate)
                        .status(PayableStatus.PENDING);
                if (rep.getType() == InvoiceType.CONTRACTOR_EXPENSE && rep.getContractor() != null) {
                    builder.contractor(rep.getContractor());
                } else {
                    builder.investorName(rep.getCompanyName());
                    if (rep.getInvestor() != null) builder.investorVoen(rep.getInvestor().getVoen());
                }
                payable = payableRepository.save(builder.build());
            } else {
                payable.setTotalAmount(total);
                if (payable.getContractor() == null
                        && (payable.getInvestorVoen() == null || payable.getInvestorVoen().isBlank())
                        && rep.getInvestor() != null && rep.getInvestor().getVoen() != null) {
                    payable.setInvestorVoen(rep.getInvestor().getVoen());
                }
            }
            recalculateStatus(payable);
        }

        // Artıq təsdiqlənmiş xərci olmayan sahiblərin payable-ını sıfırla (məs. qaimə geri qaytarıldı)
        for (Payable p : existing) {
            if (!byOwner.containsKey(ownerKey(p)) && p.getStatus() != PayableStatus.COMPLETED) {
                p.setTotalAmount(BigDecimal.ZERO);
                recalculateStatus(p);
            }
        }
    }

    /** Sahib açarı — qaimədən (podratçı id və ya investor voen/ad). */
    private static String ownerKey(Invoice i) {
        if (i.getType() == InvoiceType.CONTRACTOR_EXPENSE && i.getContractor() != null) {
            return "C:" + i.getContractor().getId();
        }
        String voen = i.getInvestor() != null ? i.getInvestor().getVoen() : null;
        return "I:" + (voen != null && !voen.isBlank() ? voen
                : (i.getCompanyName() != null ? i.getCompanyName() : "?"));
    }

    /** Sahib açarı — payable-dan (eyni məntiq). */
    private static String ownerKey(Payable p) {
        if (p.getContractor() != null) return "C:" + p.getContractor().getId();
        String voen = p.getInvestorVoen();
        return "I:" + (voen != null && !voen.isBlank() ? voen
                : (p.getInvestorName() != null ? p.getInvestorName() : "?"));
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
        // Validation: Ödəniş tarixi qaimə tarixindən əvvəl ola bilməz
        if (req.getPaymentDate() != null && invoice.getInvoiceDate() != null
                && req.getPaymentDate().isBefore(invoice.getInvoiceDate())) {
            throw new RuntimeException(
                    "Ödəniş tarixi (" + req.getPaymentDate() + ") qaimə tarixindən ("
                    + invoice.getInvoiceDate() + ") əvvəl ola bilməz");
        }

        // Qaimə üzrə ödəniş limitini yoxla
        BigDecimal invPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        if (invPaid.add(req.getAmount()).compareTo(invoice.getAmount()) > 0) {
            BigDecimal remaining = invoice.getAmount().subtract(invPaid);
            throw new RuntimeException("Ödəniş qaimə məbləğini keçə bilməz. Maksimum: " + remaining + " ₼");
        }

        // Self-heal: köhnə investor Payable-larında investorVoen NULL qalmış ola bilər
        // (investor app sorğusu findAllByInvestorVoen ilə işləyir) — qaimədəki investor FK-dən doldur
        if (p.getContractor() == null
                && (p.getInvestorVoen() == null || p.getInvestorVoen().isBlank())
                && invoice.getInvestor() != null
                && invoice.getInvestor().getVoen() != null) {
            p.setInvestorVoen(invoice.getInvestor().getVoen());
            payableRepository.save(p);
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

        // İnvestor portal push — ödəniş daxil olduqda (best-effort, izolə)
        portalNotificationService.onPaymentReceived(p, req.getAmount());

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
                // Yalnız BU payable-ın sahibinə aid qaimələr (digər sahiblərinkini sıfırlama)
                if (!ownerKey(inv).equals(ownerKey(p))) continue;
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
