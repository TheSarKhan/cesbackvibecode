package com.ces.erp.common.dashboard;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.entity.Receivable;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.accounting.repository.ReceivableRepository;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProjectRepository projectRepository;
    private final TechRequestRepository techRequestRepository;
    private final PendingOperationRepository pendingOperationRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceivableRepository receivableRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        List<RequestStatus> activeRequestStatuses = List.of(
                RequestStatus.DRAFT, RequestStatus.PENDING,
                RequestStatus.PM_REVIEW, RequestStatus.PM_SHORTLIST_READY,
                RequestStatus.COORDINATOR_NEGOTIATING, RequestStatus.COORDINATOR_PROPOSED,
                RequestStatus.PM_PRICE_NEGOTIATION, RequestStatus.PM_APPROVED,
                RequestStatus.ACCOUNTING_DOCS_CHECK, RequestStatus.EXECUTION_READY,
                RequestStatus.OPERATOR_ASSIGNED, RequestStatus.EQUIPMENT_DISPATCHED
        );

        List<Project> projects = projectRepository.findAllWithFinances();
        List<TechRequest> requests = techRequestRepository.findAllByDeletedFalse();
        List<Invoice> invoices = invoiceRepository.findAllActive();

        return DashboardStatsResponse.builder()
                .totalCustomers(customerRepository.countByDeletedFalse())
                .totalContractors(contractorRepository.countByDeletedFalse())
                .totalInvestors(investorRepository.countByDeletedFalse())
                .totalOperators(operatorRepository.countByDeletedFalse())
                .totalEmployees(userRepository.countByDeletedFalseAndActiveTrue())
                .availableEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.AVAILABLE))
                .rentedEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.RENTED))
                .defectiveEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.DEFECTIVE))
                .outOfServiceEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.OUT_OF_SERVICE))
                .pendingApprovals(pendingOperationRepository.countByStatusAndDeletedFalse(OperationStatus.PENDING))
                .activeRequests(techRequestRepository.countByStatusInAndDeletedFalse(activeRequestStatuses))
                .activeProjects(projectRepository.countByStatusAndDeletedFalse(ProjectStatus.ACTIVE))
                .deletedRecords(
                        customerRepository.countByDeletedTrue() +
                        contractorRepository.countByDeletedTrue() +
                        investorRepository.countByDeletedTrue() +
                        operatorRepository.countByDeletedTrue() +
                        equipmentRepository.countByDeletedTrue() +
                        techRequestRepository.countByDeletedTrue() +
                        projectRepository.countByDeletedTrue() +
                        userRepository.countByDeletedTrue()
                )
                .projects(buildProjectDtos(projects))
                .requests(buildRequestDtos(requests))
                .accountingSummary(buildSummary(invoices))
                .invoices(buildInvoiceDtos(invoices))
                .trends(buildTrends(invoices))
                .arAging(buildArAging())
                .topCustomers(buildTopCustomers(invoices))
                .build();
    }

    // ── Trends: bu ay vs keçən ay (% dəyişim) ─────────────────────────────────
    private DashboardStatsResponse.TrendsDto buildTrends(List<Invoice> invoices) {
        YearMonth cur = YearMonth.from(LocalDate.now());
        YearMonth prev = cur.minusMonths(1);
        BigDecimal curIncome  = monthSum(invoices, InvoiceType.INCOME, cur);
        BigDecimal prevIncome = monthSum(invoices, InvoiceType.INCOME, prev);
        BigDecimal curProfit  = monthProfit(invoices, cur);
        BigDecimal prevProfit = monthProfit(invoices, prev);
        return new DashboardStatsResponse.TrendsDto(pctChange(prevIncome, curIncome), pctChange(prevProfit, curProfit));
    }

    private BigDecimal monthProfit(List<Invoice> invoices, YearMonth ym) {
        return monthSum(invoices, InvoiceType.INCOME, ym)
                .subtract(monthSum(invoices, InvoiceType.CONTRACTOR_EXPENSE, ym))
                .subtract(monthSum(invoices, InvoiceType.COMPANY_EXPENSE, ym));
    }

    private BigDecimal monthSum(List<Invoice> invoices, InvoiceType type, YearMonth ym) {
        return invoices.stream()
                .filter(i -> i.getType() == type && i.getStatus() == InvoiceStatus.APPROVED)
                .filter(i -> i.getInvoiceDate() != null && YearMonth.from(i.getInvoiceDate()).equals(ym))
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal pctChange(BigDecimal prev, BigDecimal cur) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return cur != null && cur.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return cur.subtract(prev)
                .divide(prev.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    // ── AR Aging: açıq debitor borclarının yaşlandırılması ────────────────────
    private DashboardStatsResponse.ArAgingDto buildArAging() {
        LocalDate today = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO, cur = BigDecimal.ZERO,
                b30 = BigDecimal.ZERO, b60 = BigDecimal.ZERO, b90 = BigDecimal.ZERO;
        long overdueCount = 0;
        for (Receivable r : receivableRepository.findAllByDeletedFalse()) {
            BigDecimal totalAmt = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid     = r.getPaidAmount()  != null ? r.getPaidAmount()  : BigDecimal.ZERO;
            BigDecimal remaining = totalAmt.subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
            total = total.add(remaining);
            long overdueDays = r.getDueDate() != null ? ChronoUnit.DAYS.between(r.getDueDate(), today) : 0;
            if (overdueDays > 0) overdueCount++;
            if (overdueDays <= 30)      cur = cur.add(remaining);
            else if (overdueDays <= 60) b30 = b30.add(remaining);
            else if (overdueDays <= 90) b60 = b60.add(remaining);
            else                        b90 = b90.add(remaining);
        }
        return new DashboardStatsResponse.ArAgingDto(total, cur, b30, b60, b90, overdueCount);
    }

    // ── Top 5 müştəri (təsdiqlənmiş gəlir qaimələri üzrə) ─────────────────────
    private List<DashboardStatsResponse.TopCustomerDto> buildTopCustomers(List<Invoice> invoices) {
        Map<Long, TopAcc> acc = new HashMap<>();
        for (Invoice i : invoices) {
            if (i.getType() != InvoiceType.INCOME || i.getStatus() != InvoiceStatus.APPROVED) continue;
            Customer c = i.getCustomer();
            if (c == null && i.getProject() != null && i.getProject().getRequest() != null) {
                c = i.getProject().getRequest().getCustomer();
            }
            if (c == null) continue;
            final Customer cust = c;
            TopAcc a = acc.computeIfAbsent(cust.getId(), k -> new TopAcc(cust.getCompanyName()));
            a.count++;
            a.revenue = a.revenue.add(i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO);
        }
        return acc.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Long, TopAcc> e) -> e.getValue().revenue).reversed())
                .limit(5)
                .map(e -> new DashboardStatsResponse.TopCustomerDto(
                        e.getKey(), e.getValue().name, e.getValue().count, e.getValue().revenue))
                .toList();
    }

    private static final class TopAcc {
        final String name;
        long count = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        TopAcc(String name) { this.name = name; }
    }

    private List<DashboardStatsResponse.ProjectDto> buildProjectDtos(List<Project> projects) {
        return projects.stream().map(p -> new DashboardStatsResponse.ProjectDto(
                p.getId(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getRequest() != null ? p.getRequest().getCompanyName() : null,
                p.getProjectCode(),
                p.getEndDate() != null ? p.getEndDate().toString() : null
        )).toList();
    }

    private List<DashboardStatsResponse.RequestDto> buildRequestDtos(List<TechRequest> requests) {
        return requests.stream().map(r -> new DashboardStatsResponse.RequestDto(
                r.getId(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCompanyName()
        )).toList();
    }

    private DashboardStatsResponse.AccountingSummaryDto buildSummary(List<Invoice> invoices) {
        BigDecimal totalIncome      = sum(invoices, InvoiceType.INCOME);
        BigDecimal totalContractor  = sum(invoices, InvoiceType.CONTRACTOR_EXPENSE);
        BigDecimal totalCompany     = sum(invoices, InvoiceType.COMPANY_EXPENSE);
        return new DashboardStatsResponse.AccountingSummaryDto(
                totalIncome,
                totalContractor,
                totalCompany,
                totalIncome.subtract(totalContractor).subtract(totalCompany),
                count(invoices, InvoiceType.INCOME),
                count(invoices, InvoiceType.CONTRACTOR_EXPENSE),
                count(invoices, InvoiceType.COMPANY_EXPENSE)
        );
    }

    private List<DashboardStatsResponse.InvoiceDto> buildInvoiceDtos(List<Invoice> invoices) {
        return invoices.stream()
                .filter(i -> i.getType() == InvoiceType.INCOME)
                .map(i -> new DashboardStatsResponse.InvoiceDto(
                        i.getInvoiceDate() != null ? i.getInvoiceDate().toString() : null,
                        i.getAmount()
                )).toList();
    }

    private BigDecimal sum(List<Invoice> list, InvoiceType type) {
        return list.stream()
                .filter(i -> i.getType() == type)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(List<Invoice> list, InvoiceType type) {
        return list.stream().filter(i -> i.getType() == type).count();
    }
}
