package com.ces.erp.hr.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.PayrollStatus;
import com.ces.erp.hr.dto.PayrollEntryRequest;
import com.ces.erp.hr.dto.PayrollEntryResponse;
import com.ces.erp.hr.dto.PayrollPeriodRequest;
import com.ces.erp.hr.dto.PayrollPeriodResponse;
import com.ces.erp.hr.entity.Employee;
import com.ces.erp.hr.entity.PayrollEntry;
import com.ces.erp.hr.entity.PayrollPeriod;
import com.ces.erp.hr.repository.EmployeeRepository;
import com.ces.erp.hr.repository.PayrollEntryRepository;
import com.ces.erp.hr.repository.PayrollPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private final PayrollPeriodRepository periodRepository;
    private final PayrollEntryRepository entryRepository;
    private final EmployeeRepository employeeRepository;
    private final DeductionConfigService deductionConfigService;
    private final PayrollCalculatorService calculator;
    private final AuditService auditService;

    // ─── Period CRUD ──
    public List<PayrollPeriodResponse> getAll() {
        return periodRepository.findAllByDeletedFalseOrderByYearDescMonthDesc().stream()
                .map(p -> PayrollPeriodResponse.from(p, false))
                .toList();
    }

    public PagedResponse<PayrollPeriodResponse> getPaged(int page, int size, Integer year, PayrollStatus status) {
        Page<PayrollPeriod> result = periodRepository.searchPaged(year, status, PageRequest.of(page, size));
        return PagedResponse.<PayrollPeriodResponse>builder()
                .content(result.getContent().stream().map(p -> PayrollPeriodResponse.from(p, false)).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public PayrollPeriodResponse getById(Long id) {
        return PayrollPeriodResponse.from(loadActive(id), true);
    }

    /**
     * Yeni dövr yaradır. Auto-populate=true olduqda bütün aktiv işçilər üçün PayrollEntry əlavə edib hesablayır.
     */
    @Transactional
    public PayrollPeriodResponse createPeriod(PayrollPeriodRequest req, boolean autoPopulate) {
        if (periodRepository.existsByYearAndMonthAndDeletedFalse(req.getYear(), req.getMonth())) {
            throw new DuplicateResourceException(req.getMonth() + "/" + req.getYear() + " dövrü artıq mövcuddur");
        }
        PayrollPeriod p = PayrollPeriod.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .workingDaysInMonth(req.getWorkingDaysInMonth() != null ? req.getWorkingDaysInMonth() : 22)
                .workingHoursPerDay(req.getWorkingHoursPerDay() != null ? req.getWorkingHoursPerDay() : 8)
                .status(PayrollStatus.DRAFT)
                .notes(req.getNotes())
                .build();
        PayrollPeriod saved = periodRepository.save(p);

        if (autoPopulate) {
            populatePeriod(saved);
        }

        auditService.log("HR_PAYROLL", saved.getId(), saved.getMonth() + "/" + saved.getYear(),
                "YARADILDI", "Dövr yaradıldı" + (autoPopulate ? " və avtomatik doldurulub" : ""));
        return PayrollPeriodResponse.from(saved, true);
    }

    @Transactional
    public PayrollPeriodResponse populate(Long periodId) {
        PayrollPeriod p = loadActive(periodId);
        ensureEditable(p);
        populatePeriod(p);
        recalcTotals(p);
        return PayrollPeriodResponse.from(p, true);
    }

    private void populatePeriod(PayrollPeriod p) {
        ResolvedDeductionConfig cfg = resolveCfg(p);
        List<Employee> activeEmployees = employeeRepository.findAllByStatusAndDeletedFalse(EmployeeStatus.ACTIVE);
        for (Employee emp : activeEmployees) {
            if (entryRepository.findByPeriodIdAndEmployeeIdAndDeletedFalse(p.getId(), emp.getId()).isPresent()) {
                continue;
            }
            PayrollEntry entry = PayrollEntry.builder()
                    .period(p)
                    .employee(emp)
                    .employeeFullName(emp.getFullName())
                    .positionName(emp.getPosition() != null ? emp.getPosition().getName() : null)
                    .baseSalary(emp.getGrossSalary() != null ? emp.getGrossSalary() : BigDecimal.ZERO)
                    .workingDaysInMonth(p.getWorkingDaysInMonth())
                    .actualDaysWorked(p.getWorkingDaysInMonth())
                    .build();
            calculator.recalculate(entry, cfg);
            entryRepository.save(entry);
            p.getEntries().add(entry);
        }
    }

    @Transactional
    public PayrollPeriodResponse updatePeriod(Long periodId, PayrollPeriodRequest req) {
        PayrollPeriod p = loadActive(periodId);
        ensureEditable(p);
        if (req.getWorkingDaysInMonth() != null) p.setWorkingDaysInMonth(req.getWorkingDaysInMonth());
        if (req.getWorkingHoursPerDay() != null) p.setWorkingHoursPerDay(req.getWorkingHoursPerDay());
        if (req.getNotes() != null) p.setNotes(req.getNotes());
        // Working days dəyişdirilibsə bütün entry-lərin workingDaysInMonth-unu sinxronlaşdırırıq
        ResolvedDeductionConfig cfg = resolveCfg(p);
        if (req.getWorkingDaysInMonth() != null) {
            for (PayrollEntry e : p.getEntries()) {
                if (e.isDeleted()) continue;
                e.setWorkingDaysInMonth(req.getWorkingDaysInMonth());
                calculator.recalculate(e, cfg);
                entryRepository.save(e);
            }
        }
        recalcTotals(p);
        periodRepository.save(p);
        return PayrollPeriodResponse.from(p, true);
    }

    @Transactional
    public PayrollPeriodResponse approve(Long periodId) {
        PayrollPeriod p = loadActive(periodId);
        if (p.getStatus() != PayrollStatus.DRAFT) {
            throw new BusinessException("Yalnız DRAFT dövrlər təsdiqlənə bilər");
        }
        if (p.getEntries() == null || p.getEntries().stream().filter(e -> !e.isDeleted()).findAny().isEmpty()) {
            throw new BusinessException("Boş dövr təsdiqlənə bilməz");
        }
        // Son hesablama
        ResolvedDeductionConfig cfg = resolveCfg(p);
        for (PayrollEntry e : p.getEntries()) {
            if (e.isDeleted()) continue;
            calculator.recalculate(e, cfg);
            entryRepository.save(e);
        }
        recalcTotals(p);
        p.setStatus(PayrollStatus.APPROVED);
        p.setApprovedAt(LocalDateTime.now());
        p.setApprovedBy(currentUser());
        periodRepository.save(p);
        auditService.log("HR_PAYROLL", p.getId(), p.getMonth() + "/" + p.getYear(),
                "TƏSDİQLƏNDİ", "Aylıq əməkhaqqı təsdiqləndi");
        return PayrollPeriodResponse.from(p, true);
    }

    @Transactional
    public PayrollPeriodResponse markPaid(Long periodId) {
        PayrollPeriod p = loadActive(periodId);
        if (p.getStatus() != PayrollStatus.APPROVED) {
            throw new BusinessException("Yalnız təsdiqlənmiş dövr ödənmiş kimi qeyd oluna bilər");
        }
        p.setStatus(PayrollStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        periodRepository.save(p);
        auditService.log("HR_PAYROLL", p.getId(), p.getMonth() + "/" + p.getYear(), "ÖDƏNİLDİ", "Əməkhaqqı ödənildi");
        return PayrollPeriodResponse.from(p, true);
    }

    @Transactional
    public PayrollPeriodResponse reopen(Long periodId) {
        PayrollPeriod p = loadActive(periodId);
        if (p.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException("Ödənilmiş dövrü yenidən açmaq olmaz");
        }
        p.setStatus(PayrollStatus.DRAFT);
        p.setApprovedAt(null);
        p.setApprovedBy(null);
        periodRepository.save(p);
        return PayrollPeriodResponse.from(p, true);
    }

    @Transactional
    public void deletePeriod(Long periodId) {
        PayrollPeriod p = loadActive(periodId);
        if (p.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException("Ödənilmiş dövrü silmək olmaz");
        }
        for (PayrollEntry e : p.getEntries()) {
            if (!e.isDeleted()) e.softDelete();
        }
        p.softDelete();
        periodRepository.save(p);
        auditService.log("HR_PAYROLL", p.getId(), p.getMonth() + "/" + p.getYear(), "SİLİNDİ", "Dövr silindi");
    }

    // ─── Entry əməliyyatları ──

    @Transactional
    public PayrollEntryResponse updateEntry(Long entryId, PayrollEntryRequest req) {
        PayrollEntry e = entryRepository.findByIdAndDeletedFalse(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll entry", entryId));
        ensureEditable(e.getPeriod());
        if (req.getActualDaysWorked() != null) e.setActualDaysWorked(req.getActualDaysWorked());
        if (req.getExtraHours() != null) e.setExtraHours(req.getExtraHours());
        if (req.getOvertimePay() != null) e.setOvertimePay(req.getOvertimePay());
        if (req.getBonus() != null) e.setBonus(req.getBonus());
        if (req.getVacationPay() != null) e.setVacationPay(req.getVacationPay());
        if (req.getPenalty() != null) e.setPenalty(req.getPenalty());
        if (req.getBaseSalary() != null) e.setBaseSalary(req.getBaseSalary());
        if (req.getNotes() != null) e.setNotes(req.getNotes());

        ResolvedDeductionConfig cfg = resolveCfg(e.getPeriod());
        calculator.recalculate(e, cfg);
        entryRepository.save(e);
        recalcTotals(e.getPeriod());
        periodRepository.save(e.getPeriod());
        return PayrollEntryResponse.from(e);
    }

    @Transactional
    public PayrollEntryResponse addEntryForEmployee(Long periodId, Long employeeId) {
        PayrollPeriod p = loadActive(periodId);
        ensureEditable(p);
        if (entryRepository.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, employeeId).isPresent()) {
            throw new DuplicateResourceException("Bu işçi onsuz da bu dövrdə var");
        }
        Employee emp = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("İşçi", employeeId));

        PayrollEntry entry = PayrollEntry.builder()
                .period(p)
                .employee(emp)
                .employeeFullName(emp.getFullName())
                .positionName(emp.getPosition() != null ? emp.getPosition().getName() : null)
                .baseSalary(emp.getGrossSalary() != null ? emp.getGrossSalary() : BigDecimal.ZERO)
                .workingDaysInMonth(p.getWorkingDaysInMonth())
                .actualDaysWorked(p.getWorkingDaysInMonth())
                .build();
        calculator.recalculate(entry, resolveCfg(p));
        PayrollEntry saved = entryRepository.save(entry);
        p.getEntries().add(saved);
        recalcTotals(p);
        periodRepository.save(p);
        return PayrollEntryResponse.from(saved);
    }

    @Transactional
    public void removeEntry(Long entryId) {
        PayrollEntry e = entryRepository.findByIdAndDeletedFalse(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll entry", entryId));
        ensureEditable(e.getPeriod());
        e.softDelete();
        entryRepository.save(e);
        recalcTotals(e.getPeriod());
        periodRepository.save(e.getPeriod());
    }

    public PayrollEntryResponse getEntry(Long entryId) {
        PayrollEntry e = entryRepository.findByIdAndDeletedFalse(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll entry", entryId));
        return PayrollEntryResponse.from(e);
    }

    public List<PayrollEntryResponse> getEntriesByEmployee(Long employeeId) {
        return entryRepository.findAllByEmployeeIdAndDeletedFalseOrderByCreatedAtDesc(employeeId).stream()
                .map(PayrollEntryResponse::from)
                .toList();
    }

    // ─── Köməkçilər ──

    private PayrollPeriod loadActive(Long id) {
        return periodRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", id));
    }

    /** Dövrün tarixinə (ayın 1-i) uyğun qüvvədə olan tutulma konfiqurasiyasını həll edir. */
    private ResolvedDeductionConfig resolveCfg(PayrollPeriod p) {
        return deductionConfigService.resolveForDate(LocalDate.of(p.getYear(), p.getMonth(), 1));
    }

    private void ensureEditable(PayrollPeriod p) {
        if (p.getStatus() != PayrollStatus.DRAFT) {
            throw new BusinessException("Yalnız DRAFT dövr redaktə oluna bilər");
        }
    }

    private void recalcTotals(PayrollPeriod p) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal empDed = BigDecimal.ZERO;
        BigDecimal empContrib = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        for (PayrollEntry e : p.getEntries()) {
            if (e.isDeleted()) continue;
            gross = gross.add(nz(e.getGrossTotal()));
            net = net.add(nz(e.getNetPay()));
            empDed = empDed.add(nz(e.getTotalDeductions()));
            empContrib = empContrib.add(nz(e.getTotalEmployerContributions()));
            income = income.add(nz(e.getIncomeTax()));
        }
        p.setTotalGross(gross);
        p.setTotalNet(net);
        p.setTotalEmployeeDeductions(empDed);
        p.setTotalEmployerContributions(empContrib);
        p.setTotalIncomeTax(income);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String currentUser() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            return a != null ? a.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
