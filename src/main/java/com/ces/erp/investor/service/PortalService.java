package com.ces.erp.investor.service;

import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.dto.PayablePaymentResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.entity.Payable;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.accounting.repository.PayablePaymentRepository;
import com.ces.erp.accounting.repository.PayableRepository;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.enums.PayableStatus;
import com.ces.erp.garage.dto.DocumentResponse;
import com.ces.erp.garage.dto.EquipmentResponse;
import com.ces.erp.garage.dto.ProjectHistoryResponse;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.entity.EquipmentDocument;
import com.ces.erp.garage.repository.EquipmentDocumentRepository;
import com.ces.erp.garage.repository.EquipmentProjectHistoryRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.dto.PortalDashboardResponse;
import com.ces.erp.investor.dto.PortalEquipmentEarnings;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.partydoc.PartyDocumentDto;
import com.ces.erp.partydoc.PartyDocumentService;
import com.ces.erp.partydoc.PartyKind;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Portal (investor) scoped servisi. KRİTİK: bütün sorğular principal-dan gələn
 * {@code investorId} ilə süzülür — istemçi heç vaxt id/voen ötürmür.
 *   • Qaimələr: investor FK (investor_id) ilə (TƏHLÜKƏSİZ, ad ilə deyil).
 *   • Avadanlıq/ödəniş: investor VÖEN-i ilə (VÖEN unique → çakışma yox); VÖEN həmişə
 *     token-dakı investorId ilə yüklənən qeyddən götürülür, asla istemçidən.
 */
@Service
@RequiredArgsConstructor
public class PortalService {

    private final InvestorRepository investorRepository;
    private final InvoiceRepository invoiceRepository;
    private final PayableRepository payableRepository;
    private final PayablePaymentRepository payablePaymentRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentProjectHistoryRepository projectHistoryRepository;
    private final EquipmentDocumentRepository equipmentDocumentRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final PartyDocumentService partyDocumentService;

    private Investor me(Long investorId) {
        return investorRepository.findByIdAndDeletedFalse(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("İnvestor", investorId));
    }

    @Transactional(readOnly = true)
    public InvestorResponse getProfile(Long investorId) {
        return InvestorResponse.from(me(investorId));
    }

    @Transactional(readOnly = true)
    public PortalDashboardResponse getDashboard(Long investorId) {
        Investor investor = me(investorId);
        List<Equipment> equipment = myEquipment(investor);

        Map<String, Long> byStatus = equipment.stream()
                .collect(Collectors.groupingBy(e -> e.getStatus().name(), Collectors.counting()));

        List<Invoice> invoices = invoiceRepository.findAllByInvestorId(investorId);
        BigDecimal totalInvoiced = invoices.stream()
                .map(Invoice::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payable> payables = payableRepository.findAllByInvestorVoen(investor.getVoen());
        BigDecimal totalPaid = payables.stream()
                .map(Payable::getPaidAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableTotal = payables.stream()
                .map(Payable::getTotalAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long openCount = payables.stream()
                .filter(p -> p.getStatus() != PayableStatus.COMPLETED)
                .count();

        return PortalDashboardResponse.builder()
                .equipmentCount(equipment.size())
                .equipmentByStatus(byStatus)
                .totalInvoiced(totalInvoiced)
                .totalPaid(totalPaid)
                .outstanding(payableTotal.subtract(totalPaid))
                .openPayablesCount(openCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getEquipment(Long investorId) {
        return myEquipment(me(investorId)).stream()
                .map(EquipmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getEquipmentDetail(Long investorId, Long equipmentId) {
        return EquipmentResponse.from(ownedEquipment(me(investorId), equipmentId));
    }

    @Transactional(readOnly = true)
    public List<ProjectHistoryResponse> getEquipmentHistory(Long investorId, Long equipmentId) {
        ownedEquipment(me(investorId), equipmentId); // sahiblik yoxlaması
        return projectHistoryRepository.findAllByEquipmentIdOrderByStartDateDesc(equipmentId).stream()
                .map(ProjectHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PortalEquipmentEarnings getEquipmentEarnings(Long investorId, Long equipmentId) {
        ownedEquipment(me(investorId), equipmentId); // sahiblik yoxlaması (başqasının texnikası → 404)

        // Texnikaya ID ilə bağlı investor qazanc (INVESTOR_EXPENSE) qaimələri — köhnədən yeniyə
        List<Invoice> earnings = invoiceRepository.findInvestorEarningsByEquipmentId(equipmentId);

        BigDecimal total = earnings.stream()
                .map(Invoice::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate now = LocalDate.now();

        BigDecimal monthEarn = earnings.stream()
                .filter(i -> { int[] ym = periodOf(i); return ym[0] == now.getYear() && ym[1] == now.getMonthValue(); })
                .map(i -> nz(i.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Trend — son 12 ay (köhnədən yeniyə), boş aylar 0
        Map<String, BigDecimal> byMonth = new HashMap<>();
        for (Invoice i : earnings) {
            int[] ym = periodOf(i);
            byMonth.merge(ym[0] + "-" + ym[1], nz(i.getAmount()), BigDecimal::add);
        }
        List<PortalEquipmentEarnings.MonthPoint> trend = new ArrayList<>();
        YearMonth cursor = YearMonth.of(now.getYear(), now.getMonthValue()).minusMonths(11);
        for (int k = 0; k < 12; k++) {
            trend.add(PortalEquipmentEarnings.MonthPoint.builder()
                    .year(cursor.getYear())
                    .month(cursor.getMonthValue())
                    .amount(byMonth.getOrDefault(cursor.getYear() + "-" + cursor.getMonthValue(), BigDecimal.ZERO))
                    .build());
            cursor = cursor.plusMonths(1);
        }

        // Günlük dərəcə — son qaimənin aylıq dərəcəsindən təxmini
        BigDecimal dailyRate = null;
        if (!earnings.isEmpty()) {
            Invoice last = earnings.get(earnings.size() - 1);
            if (last.getMonthlyRate() != null) {
                int wd = (last.getWorkingDaysInMonth() != null && last.getWorkingDaysInMonth() > 0)
                        ? last.getWorkingDaysInMonth() : 26;
                dailyRate = last.getMonthlyRate().divide(BigDecimal.valueOf(wd), 2, RoundingMode.HALF_UP);
            }
        }

        return PortalEquipmentEarnings.builder()
                .equipmentId(equipmentId)
                .totalEarn(total)
                .monthEarn(monthEarn)
                .dailyRate(dailyRate)
                .utilizationPct(computeUtilizationPct(equipmentId, now))
                .trend(trend)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PartyDocumentDto> getDocuments(Long investorId) {
        // Sənəd mərkəzi — investorun BÜTÜN sənədləri (sahib müqaviləsi/protokol,
        // təhvil-təslim aktları, texnika qaraj sənədləri, qaimələr, əl ilə yüklənənlər).
        // me() ilə investorun mövcudluğu təsdiqlənir; aqreqasiya yalnız bu investorId üzrə.
        me(investorId);
        return partyDocumentService.collect(PartyKind.INVESTOR, investorId);
    }

    /**
     * Sənəd mərkəzindən faylın fiziki yolu + adı — yalnız bu investora aid sənəd üçün.
     * Sahiblik {@link PartyDocumentService}-də yoxlanır; aid deyilsə 404.
     */
    @Transactional(readOnly = true)
    public PartyDocumentService.DownloadFile resolveHubDocument(Long investorId, String sourceType, Long sourceId) {
        me(investorId);
        return partyDocumentService.resolveDownload(PartyKind.INVESTOR, investorId, sourceType, sourceId);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long investorId) {
        // TƏHLÜKƏSİZ: investor FK ilə (companyName ilə deyil)
        // Yalnız rəsmən verilmiş qaimələr (göndərilmiş/təsdiqlənmiş) — qaralama görünməsin.
        return invoiceRepository.findAllByInvestorId(investorId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.SENT || i.getStatus() == InvoiceStatus.APPROVED)
                .map(InvoiceResponse::from)
                .toList();
    }

    /** Konkret qaimə üzrə edilmiş ödənişlər — yalnız bu investorun qaiməsi üçün (yoxsa 404). */
    @Transactional(readOnly = true)
    public List<PayablePaymentResponse> getInvoicePayments(Long investorId, Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .filter(i -> !i.isDeleted() && i.getInvestor() != null
                        && i.getInvestor().getId().equals(investorId))
                .orElseThrow(() -> new ResourceNotFoundException("Qaimə", invoiceId));
        return payablePaymentRepository
                .findAllByInvoiceIdAndDeletedFalseOrderByPaymentDateAsc(inv.getId()).stream()
                .map(PayablePaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayableResponse> getPayments(Long investorId) {
        Investor investor = me(investorId);
        return payableRepository.findAllByInvestorVoen(investor.getVoen()).stream()
                .map(p -> {
                    List<Invoice> invoices = p.getProject() != null
                            ? invoiceRepository.findAllByProjectIdAndDeletedFalse(p.getProject().getId())
                            : List.of();
                    return PayableResponse.from(p, invoices);
                })
                .toList();
    }

    @Transactional
    public void changePassword(Long investorId, String oldPassword, String newPassword) {
        Investor investor = me(investorId);
        if (investor.getPasswordHash() == null
                || !passwordEncoder.matches(oldPassword, investor.getPasswordHash())) {
            throw new BusinessException("Köhnə şifrə yanlışdır");
        }
        investor.setPasswordHash(passwordEncoder.encode(newPassword));
        investorRepository.save(investor);
    }

    // ─── Köməkçilər ──────────────────────────────────────────────────────────

    /** İnvestorun avadanlıqları — yalnız VÖEN üzrə (unique, təhlükəsiz). */
    private List<Equipment> myEquipment(Investor investor) {
        return equipmentRepository.findAllByInvestor(investor.getVoen(), null);
    }

    /** Verilmiş avadanlıq həqiqətən bu investora aiddirmi? Deyilsə 404. */
    private Equipment ownedEquipment(Investor investor, Long equipmentId) {
        Equipment e = equipmentRepository.findByIdAndDeletedFalse(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Avadanlıq", equipmentId));
        if (e.getOwnerInvestorVoen() == null || !e.getOwnerInvestorVoen().equals(investor.getVoen())) {
            // Başqasının avadanlığı — mövcudluğu sızdırmamaq üçün 404
            throw new ResourceNotFoundException("Avadanlıq", equipmentId);
        }
        return e;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Qaimənin aid olduğu (il, ay) — period sahələri, yoxdursa qaimə tarixi. */
    private static int[] periodOf(Invoice i) {
        if (i.getPeriodYear() != null && i.getPeriodMonth() != null) {
            return new int[]{ i.getPeriodYear(), i.getPeriodMonth() };
        }
        LocalDate d = i.getInvoiceDate();
        return new int[]{ d.getYear(), d.getMonthValue() };
    }

    /** Son 12 ayda işləklik % — layihə tarixçəsindəki icarə günlərinin 365 günə nisbəti. */
    private int computeUtilizationPct(Long equipmentId, LocalDate now) {
        LocalDate windowStart = now.minusDays(364); // 365 günlük pəncərə (daxil)
        long rentedDays = 0;
        for (var h : projectHistoryRepository.findAllByEquipmentIdOrderByStartDateDesc(equipmentId)) {
            LocalDate s = h.getStartDate();
            if (s == null) continue;
            LocalDate e = h.getEndDate() != null ? h.getEndDate() : now; // davam edən → bu gün
            LocalDate from = s.isBefore(windowStart) ? windowStart : s;
            LocalDate to = e.isAfter(now) ? now : e;
            if (!to.isBefore(from)) {
                rentedDays += ChronoUnit.DAYS.between(from, to) + 1;
            }
        }
        long pct = Math.round(rentedDays * 100.0 / 365.0);
        return (int) Math.min(100, Math.max(0, pct));
    }
}
