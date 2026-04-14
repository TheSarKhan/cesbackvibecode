package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.report.*;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.enums.InvoiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final com.ces.erp.accounting.repository.ReceivableRepository receivableRepository;

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        List<Invoice> activeInvoices = invoiceRepository.findAllActiveWithDateRange(startDate, endDate);
        return calculateSummary(activeInvoices);
    }

    private ReportSummaryResponse calculateSummary(List<Invoice> invoices) {
        BigDecimal totalIncome = calculateTotal(invoices, InvoiceType.INCOME);
        BigDecimal totalContractorExpense = calculateTotal(invoices, InvoiceType.CONTRACTOR_EXPENSE);
        BigDecimal totalCompanyExpense = calculateTotal(invoices, InvoiceType.COMPANY_EXPENSE);
        BigDecimal totalExpense = totalContractorExpense.add(totalCompanyExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        long incomeCount = countInvoices(invoices, InvoiceType.INCOME);
        long contractorCount = countInvoices(invoices, InvoiceType.CONTRACTOR_EXPENSE);
        long companyCount = countInvoices(invoices, InvoiceType.COMPANY_EXPENSE);

        BigDecimal avgInvoiceAmount = BigDecimal.ZERO;
        if (!invoices.isEmpty()) {
            BigDecimal totalAmount = totalIncome.add(totalExpense);
            avgInvoiceAmount = totalAmount.divide(BigDecimal.valueOf(invoices.size()), 2, RoundingMode.HALF_UP);
        }

        return ReportSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalContractorExpense(totalContractorExpense)
                .totalCompanyExpense(totalCompanyExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .incomeCount(incomeCount)
                .contractorExpenseCount(contractorCount)
                .companyExpenseCount(companyCount)
                .avgInvoiceAmount(avgInvoiceAmount)
                .build();
    }

    private BigDecimal calculateTotal(List<Invoice> invoices, InvoiceType type) {
        return invoices.stream()
                .filter(i -> i.getType() == type)
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countInvoices(List<Invoice> invoices, InvoiceType type) {
        return invoices.stream().filter(i -> i.getType() == type).count();
    }

    @Transactional(readOnly = true)
    public PeriodComparisonResponse getComparison(LocalDate currentStart, LocalDate currentEnd, LocalDate prevStart, LocalDate prevEnd) {
        List<Invoice> currentInvoices = invoiceRepository.findAllActiveWithDateRange(currentStart, currentEnd);
        List<Invoice> prevInvoices = invoiceRepository.findAllActiveWithDateRange(prevStart, prevEnd);

        ReportSummaryResponse currentPeriod = calculateSummary(currentInvoices);
        ReportSummaryResponse previousPeriod = calculateSummary(prevInvoices);

        currentPeriod.setIncomeChangeScore(calculateChange(currentPeriod.getTotalIncome(), previousPeriod.getTotalIncome()));
        currentPeriod.setExpenseChangeScore(calculateChange(currentPeriod.getTotalExpense(), previousPeriod.getTotalExpense()));
        currentPeriod.setProfitChangeScore(calculateChange(currentPeriod.getNetProfit(), previousPeriod.getNetProfit()));

        return PeriodComparisonResponse.builder()
                .currentPeriodLabel(currentStart + " to " + currentEnd)
                .previousPeriodLabel(prevStart + " to " + prevEnd)
                .currentPeriod(currentPeriod)
                .previousPeriod(previousPeriod)
                .build();
    }

    private Double calculateChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        BigDecimal difference = current.subtract(previous);
        return difference.divide(previous.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendItem> getMonthlyTrend(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findAllActiveWithDateRange(startDate, endDate);
        
        Map<YearMonth, MonthlyTrendItem> trendMap = new TreeMap<>();
        
        // Initialize map with all months in range if possible, but for simplicity we'll just populate from data.
        // It's better to populate all months.
        if (startDate != null && endDate != null) {
            YearMonth currentMonth = YearMonth.from(startDate);
            YearMonth endMonth = YearMonth.from(endDate);
            while (!currentMonth.isAfter(endMonth)) {
                trendMap.put(currentMonth, MonthlyTrendItem.builder()
                        .month(currentMonth.toString())
                        .monthNum(currentMonth.getMonthValue())
                        .year(currentMonth.getYear())
                        .income(BigDecimal.ZERO)
                        .contractorExpense(BigDecimal.ZERO)
                        .companyExpense(BigDecimal.ZERO)
                        .netProfit(BigDecimal.ZERO)
                        .build());
                currentMonth = currentMonth.plusMonths(1);
            }
        }

        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceDate() == null) continue;
            YearMonth ym = YearMonth.from(invoice.getInvoiceDate());
            trendMap.putIfAbsent(ym, MonthlyTrendItem.builder()
                    .month(ym.toString())
                    .monthNum(ym.getMonthValue())
                    .year(ym.getYear())
                    .income(BigDecimal.ZERO)
                    .contractorExpense(BigDecimal.ZERO)
                    .companyExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .build());

            MonthlyTrendItem item = trendMap.get(ym);
            BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
            if (invoice.getType() == InvoiceType.INCOME) {
                item.setIncome(item.getIncome().add(amount));
                item.setNetProfit(item.getNetProfit().add(amount));
            } else if (invoice.getType() == InvoiceType.CONTRACTOR_EXPENSE) {
                item.setContractorExpense(item.getContractorExpense().add(amount));
                item.setNetProfit(item.getNetProfit().subtract(amount));
            } else if (invoice.getType() == InvoiceType.COMPANY_EXPENSE) {
                item.setCompanyExpense(item.getCompanyExpense().add(amount));
                item.setNetProfit(item.getNetProfit().subtract(amount));
            }
        }

        return new ArrayList<>(trendMap.values());
    }

    @Transactional(readOnly = true)
    public List<ProjectReportItem> getProjectReport(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findAllActiveWithDateRange(startDate, endDate);
        
        Map<Long, ProjectReportItem> map = new HashMap<>();
        
        for (Invoice invoice : invoices) {
            if (invoice.getProject() == null) continue;
            Long projectId = invoice.getProject().getId();
            
            map.putIfAbsent(projectId, ProjectReportItem.builder()
                    .projectId(projectId)
                    .projectCode(invoice.getProject().getProjectCode())
                    .companyName(invoice.getProject().getRequest() != null ? invoice.getProject().getRequest().getCompanyName() : "")
                    .projectName(invoice.getProject().getRequest() != null ? invoice.getProject().getRequest().getProjectName() : "")
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .profitMarginPercent(0.0)
                    .invoiceCount(0L)
                    .build());
                    
            ProjectReportItem item = map.get(projectId);
            BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
            
            if (invoice.getType() == InvoiceType.INCOME) {
                item.setTotalIncome(item.getTotalIncome().add(amount));
                item.setNetProfit(item.getNetProfit().add(amount));
            } else {
                item.setTotalExpense(item.getTotalExpense().add(amount));
                item.setNetProfit(item.getNetProfit().subtract(amount));
            }
            item.setInvoiceCount(item.getInvoiceCount() + 1);
            
            if (item.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
                item.setProfitMarginPercent(item.getNetProfit().divide(item.getTotalIncome(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
            }
        }
        
        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<PartnerReportItem> getPartnerReport(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findAllActiveWithDateRange(startDate, endDate);
        
        Map<String, PartnerReportItem> map = new HashMap<>(); // Key: "TYPE_ID"
        
        for (Invoice invoice : invoices) {
            if (invoice.getType() != InvoiceType.CONTRACTOR_EXPENSE) continue;
            if (invoice.getContractor() == null && invoice.getInvestor() == null) continue;
            
            boolean isContractor = invoice.getContractor() != null;
            String type = isContractor ? "CONTRACTOR" : "INVESTOR";
            Long id = isContractor ? invoice.getContractor().getId() : invoice.getInvestor().getId();
            String key = type + "_" + id;
            
            map.putIfAbsent(key, PartnerReportItem.builder()
                    .type(type)
                    .id(id)
                    .name(isContractor ? invoice.getContractor().getCompanyName() : invoice.getInvestor().getCompanyName())
                    .voen(isContractor ? invoice.getContractor().getVoen() : invoice.getInvestor().getVoen())
                    .totalExpense(BigDecimal.ZERO)
                    .invoiceCount(0L)
                    .lastPaymentDate(invoice.getInvoiceDate())
                    .build());
                    
            PartnerReportItem item = map.get(key);
            BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
            
            item.setTotalExpense(item.getTotalExpense().add(amount));
            item.setInvoiceCount(item.getInvoiceCount() + 1);
            
            if (invoice.getInvoiceDate() != null && (item.getLastPaymentDate() == null || invoice.getInvoiceDate().isAfter(item.getLastPaymentDate()))) {
                item.setLastPaymentDate(invoice.getInvoiceDate());
            }
        }
        
        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<ExpenseBreakdownItem> getExpenseBreakdown(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findAllActiveWithDateRange(startDate, endDate);
        
        BigDecimal totalContractor = calculateTotal(invoices, InvoiceType.CONTRACTOR_EXPENSE);
        BigDecimal totalCompany = calculateTotal(invoices, InvoiceType.COMPANY_EXPENSE);
        BigDecimal totalExpense = totalContractor.add(totalCompany);
        
        long contractorCount = countInvoices(invoices, InvoiceType.CONTRACTOR_EXPENSE);
        long companyCount = countInvoices(invoices, InvoiceType.COMPANY_EXPENSE);
        
        List<ExpenseBreakdownItem> list = new ArrayList<>();
        
        if (totalContractor.compareTo(BigDecimal.ZERO) > 0) {
            list.add(ExpenseBreakdownItem.builder()
                    .category("CONTRACTOR_EXPENSE")
                    .categoryLabel("Podratçı / İnvestor xərcləri")
                    .amount(totalContractor)
                    .percentage(totalContractor.divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                    .count(contractorCount)
                    .build());
        }
        
        if (totalCompany.compareTo(BigDecimal.ZERO) > 0) {
            list.add(ExpenseBreakdownItem.builder()
                    .category("COMPANY_EXPENSE")
                    .categoryLabel("Şirkət xərcləri")
                    .amount(totalCompany)
                    .percentage(totalCompany.divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                    .count(companyCount)
                    .build());
        }
        
        return list;
    }

    @Transactional(readOnly = true)
    public List<CashFlowItem> getCashFlow(LocalDate startDate, LocalDate endDate) {
        List<MonthlyTrendItem> trends = getMonthlyTrend(startDate, endDate);
        
        return trends.stream()
                .map(t -> CashFlowItem.builder()
                        .month(t.getMonth())
                        .monthNum(t.getMonthNum())
                        .year(t.getYear())
                        .inflow(t.getIncome())
                        .outflow(t.getContractorExpense().add(t.getCompanyExpense()))
                        .net(t.getNetProfit())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceivableReportItem> getReceivableReport(LocalDate startDate, LocalDate endDate) {
        List<com.ces.erp.accounting.entity.Receivable> all = receivableRepository.findAll();
        return all.stream()
                .filter(r -> !r.isDeleted())
                .filter(r -> {
                    if (startDate != null && endDate != null && r.getDueDate() != null) {
                        return !r.getDueDate().isBefore(startDate) && !r.getDueDate().isAfter(endDate);
                    }
                    return true;
                })
                .map(r -> ReceivableReportItem.builder()
                        .id(r.getId())
                        .projectCode(r.getProject() != null ? r.getProject().getProjectCode() : null)
                        .customerName(r.getCustomer() != null ? r.getCustomer().getCompanyName() : null)
                        .totalAmount(r.getTotalAmount())
                        .paidAmount(r.getPaidAmount())
                        .remainingAmount(r.getTotalAmount().subtract(r.getPaidAmount()))
                        .dueDate(r.getDueDate())
                        .status(r.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
