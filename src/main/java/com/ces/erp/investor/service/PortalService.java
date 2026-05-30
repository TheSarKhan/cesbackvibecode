package com.ces.erp.investor.service;

import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.entity.Payable;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.accounting.repository.PayableRepository;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.enums.PayableStatus;
import com.ces.erp.garage.dto.DocumentResponse;
import com.ces.erp.garage.dto.EquipmentResponse;
import com.ces.erp.garage.dto.ProjectHistoryResponse;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentDocumentRepository;
import com.ces.erp.garage.repository.EquipmentProjectHistoryRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.dto.InvestorResponse;
import com.ces.erp.investor.dto.PortalDashboardResponse;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private final EquipmentRepository equipmentRepository;
    private final EquipmentProjectHistoryRepository projectHistoryRepository;
    private final EquipmentDocumentRepository equipmentDocumentRepository;
    private final PasswordEncoder passwordEncoder;

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
    public List<DocumentResponse> getDocuments(Long investorId) {
        // Yalnız mənim avadanlıqlarıma aid sənədlər
        return myEquipment(me(investorId)).stream()
                .flatMap(e -> equipmentDocumentRepository.findAllByEquipmentId(e.getId()).stream())
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long investorId) {
        // TƏHLÜKƏSİZ: investor FK ilə (companyName ilə deyil)
        return invoiceRepository.findAllByInvestorId(investorId).stream()
                .map(InvoiceResponse::from)
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
}
