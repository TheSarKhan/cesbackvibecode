package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.AccountingSummaryResponse;
import com.ces.erp.accounting.dto.InvoiceRequest;
import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.InvalidStatusTransitionException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.websocket.NotificationService;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.project.entity.ProjectRevenue;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.project.repository.ProjectRevenueRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService implements ApprovalHandler {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRevenueRepository projectRevenueRepository;
    private final com.ces.erp.project.repository.ProjectExpenseRepository projectExpenseRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final CoordinatorPlanRepository coordinatorPlanRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ReceivableService receivableService;
    private final PayableService payableService;

    @Override public String getEntityType() { return "INVOICE"; }
    @Override public String getModuleCode()  { return "ACCOUNTING"; }
    @Override public String getLabel(Long id) {
        Invoice inv = findOrThrow(id);
        return inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "Qaimə #" + id;
    }
    @Override public Object getSnapshot(Long id) { return InvoiceResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            InvoiceRequest req = objectMapper.readValue(json, InvoiceRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    // ─── List & Summary ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAllActive().stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByType(InvoiceType type) {
        return invoiceRepository.findAllByType(type).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getAllPaged(int page, int size, String search, String type) {
        String q = (search != null && !search.isBlank()) ? search : null;
        var pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending().and(Sort.by("createdAt").descending()));
        if ("PAYMENT".equals(type)) {
            return PagedResponse.from(
                    invoiceRepository.findAllFilteredByTypes(q,
                            List.of(InvoiceType.CONTRACTOR_EXPENSE, InvoiceType.INVESTOR_EXPENSE), pageable),
                    InvoiceResponse::from);
        }
        InvoiceType t = (type != null && !type.isBlank()) ? InvoiceType.valueOf(type) : null;
        return PagedResponse.from(invoiceRepository.findAllFiltered(q, t, pageable), InvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return InvoiceResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByProjectId(Long projectId) {
        return invoiceRepository.findAllByProjectIdAndDeletedFalse(projectId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountingSummaryResponse getSummary() {
        List<Invoice> all = invoiceRepository.findAllActive();

        BigDecimal totalIncome = sum(all, InvoiceType.INCOME);
        BigDecimal totalContractor = sum(all, InvoiceType.CONTRACTOR_EXPENSE);
        BigDecimal totalCompany = sum(all, InvoiceType.COMPANY_EXPENSE);
        BigDecimal totalExpense = totalContractor.add(totalCompany);

        return AccountingSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalContractorExpense(totalContractor)
                .totalCompanyExpense(totalCompany)
                .totalExpense(totalExpense)
                .netProfit(totalIncome.subtract(totalExpense))
                .incomeCount(count(all, InvoiceType.INCOME))
                .contractorExpenseCount(count(all, InvoiceType.CONTRACTOR_EXPENSE))
                .companyExpenseCount(count(all, InvoiceType.COMPANY_EXPENSE))
                .build();
    }

    // ─── Create / Update / Delete ─────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest req) {
        validate(req, null);

        Invoice inv = Invoice.builder()
                .type(req.getType())
                .status(req.getStatus() != null ? req.getStatus() : InvoiceStatus.DRAFT)
                .invoiceNumber(req.getInvoiceNumber())
                .amount(req.getAmount())
                .invoiceDate(req.getInvoiceDate())
                .etaxesId(req.getEtaxesId())
                .equipmentName(req.getEquipmentName())
                .companyName(req.getCompanyName())
                .serviceDescription(req.getServiceDescription())
                .notes(req.getNotes())
                .build();

        inv.setPeriodMonth(req.getPeriodMonth());
        inv.setPeriodYear(req.getPeriodYear());
        inv.setStandardDays(req.getStandardDays());
        inv.setExtraDays(req.getExtraDays());
        inv.setExtraHours(req.getExtraHours());
        inv.setMonthlyRate(req.getMonthlyRate());
        inv.setWorkingDaysInMonth(req.getWorkingDaysInMonth());
        inv.setWorkingHoursPerDay(req.getWorkingHoursPerDay());
        inv.setOvertimeRate(req.getOvertimeRate());
        if (req.getType() == InvoiceType.INCOME && req.getPeriodMonth() != null) {
            BigDecimal calc = calculateTimesheetAmount(req);
            if (calc != null) inv.setAmount(calc);
        }

        if (req.getProjectId() != null) {
            inv.setProject(projectRepository.findById(req.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Layihə", req.getProjectId())));
        }
        if (req.getContractorId() != null) {
            inv.setContractor(contractorRepository.findById(req.getContractorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Podratçı", req.getContractorId())));
        }
        if (req.getInvestorId() != null) {
            inv.setInvestor(investorRepository.findById(req.getInvestorId())
                    .orElseThrow(() -> new ResourceNotFoundException("İnvestor", req.getInvestorId())));
        }

        Invoice saved = invoiceRepository.save(inv);
        if (saved.getType() == InvoiceType.INCOME) {
            receivableService.syncInvoiceDebt(saved);
        }
        notificationService.success("Yeni faktura", "Faktura yaradıldı: " + saved.getInvoiceNumber(), "ACCOUNTING");
        auditService.log("FAKTURA", saved.getId(), saved.getInvoiceNumber(), "YARADILDI", "Yeni faktura qeydiyyatı");
        return InvoiceResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "ACCOUNTING", entityType = "INVOICE")
    public InvoiceResponse update(Long id, InvoiceRequest req) {
        Invoice inv = findOrThrow(id);
        validate(req, id);

        inv.setType(req.getType());
        inv.setInvoiceNumber(req.getInvoiceNumber());
        inv.setAmount(req.getAmount());
        inv.setInvoiceDate(req.getInvoiceDate());
        inv.setEtaxesId(req.getEtaxesId());
        inv.setEquipmentName(req.getEquipmentName());
        inv.setCompanyName(req.getCompanyName());
        inv.setServiceDescription(req.getServiceDescription());
        inv.setNotes(req.getNotes());
        inv.setPeriodMonth(req.getPeriodMonth());
        inv.setPeriodYear(req.getPeriodYear());
        inv.setStandardDays(req.getStandardDays());
        inv.setExtraDays(req.getExtraDays());
        inv.setExtraHours(req.getExtraHours());
        inv.setMonthlyRate(req.getMonthlyRate());
        inv.setWorkingDaysInMonth(req.getWorkingDaysInMonth());
        inv.setWorkingHoursPerDay(req.getWorkingHoursPerDay());
        inv.setOvertimeRate(req.getOvertimeRate());
        if (req.getType() == InvoiceType.INCOME && req.getPeriodMonth() != null) {
            BigDecimal calc = calculateTimesheetAmount(req);
            if (calc != null) inv.setAmount(calc);
        }

        inv.setProject(req.getProjectId() != null
                ? projectRepository.findById(req.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Layihə", req.getProjectId()))
                : null);
        inv.setContractor(req.getContractorId() != null
                ? contractorRepository.findById(req.getContractorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Podratçı", req.getContractorId()))
                : null);
        inv.setInvestor(req.getInvestorId() != null
                ? investorRepository.findById(req.getInvestorId())
                        .orElseThrow(() -> new ResourceNotFoundException("İnvestor", req.getInvestorId()))
                : null);

        Invoice updated = invoiceRepository.save(inv);
        if (updated.getType() == InvoiceType.INCOME) {
            receivableService.syncInvoiceDebt(updated);
        }
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "YENİLƏNDİ", "Faktura məlumatları yeniləndi");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    public InvoiceResponse patchFields(Long id, com.ces.erp.accounting.dto.InvoiceFieldsRequest req) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() == InvoiceStatus.APPROVED) {
            throw new BusinessException("Təsdiqlənmiş qaimədə dəyişiklik etmək olmaz");
        }
        // APPROVED/RETURNED statuslarına yalnız approve/return endpointləri ilə keçmək olar
        if (req.getStatus() != null && (req.getStatus() == InvoiceStatus.APPROVED || req.getStatus() == InvoiceStatus.RETURNED)) {
            throw new InvalidStatusTransitionException("Bu status dəyişikliyi üçün müvafiq endpointdən istifadə edin");
        }
        InvoiceStatus prevStatus = inv.getStatus();
        if (req.getInvoiceNumber() != null) inv.setInvoiceNumber(req.getInvoiceNumber().isBlank() ? null : req.getInvoiceNumber().trim());
        if (req.getEtaxesId() != null) {
            String etaxesId = req.getEtaxesId().isBlank() ? null : req.getEtaxesId().trim();
            if (etaxesId != null && inv.getType() == InvoiceType.INCOME) {
                boolean exists = invoiceRepository.existsByEtaxesIdAndDeletedFalse(etaxesId);
                if (exists && !etaxesId.equals(inv.getEtaxesId())) {
                    throw new DuplicateResourceException("Bu ETaxes ID artıq mövcuddur: " + etaxesId);
                }
            }
            inv.setEtaxesId(etaxesId);
        }
        if (req.getInvoiceDate() != null) inv.setInvoiceDate(req.getInvoiceDate());
        if (req.getNotes() != null) inv.setNotes(req.getNotes().isBlank() ? null : req.getNotes().trim());
        if (req.getStatus() != null) inv.setStatus(req.getStatus());
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "SAHƏ YENİLƏNDİ", "Mühasib sahələri doldurdu");

        // INCOME qaimə SENT olduqda → podratçı/investor xərc qaiməsini avtomatik yarat
        if (updated.getType() == InvoiceType.INCOME
                && updated.getStatus() == InvoiceStatus.SENT
                && prevStatus != InvoiceStatus.SENT) {
            autoCreateExpenseInvoice(updated);
        }

        return InvoiceResponse.from(updated);
    }

    @Transactional
    @RequiresApproval(module = "ACCOUNTING", entityType = "INVOICE", isDelete = true)
    public void delete(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() == InvoiceStatus.SENT || inv.getStatus() == InvoiceStatus.APPROVED) {
            throw new BusinessException("Mühasibatlığa göndərilmiş və ya təsdiqlənmiş qaiməni silə bilməzsiniz");
        }
        auditService.log("FAKTURA", inv.getId(), inv.getInvoiceNumber(), "SİLİNDİ", "Faktura silindi");
        inv.softDelete();
        Invoice deleted = invoiceRepository.save(inv);
        if (deleted.getType() == InvoiceType.INCOME) {
            receivableService.syncInvoiceDebt(deleted);
        }
    }

    // ─── Approve / Return ──────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse approve(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.SENT) {
            throw new BusinessException("Yalnız göndərilmiş qaimələr təsdiqlənə bilər");
        }
        if (inv.getInvoiceNumber() == null || inv.getInvoiceNumber().isBlank()) {
            throw new BusinessException("Qaimə nömrəsi doldurulmalıdır");
        }
        if (inv.getInvoiceDate() == null) {
            throw new BusinessException("Qaimə tarixi doldurulmalıdır");
        }

        inv.setStatus(InvoiceStatus.APPROVED);
        Invoice updated = invoiceRepository.save(inv);

        // Layihənin maliyyə hissəsinə gəlir olaraq əlavə et
        if (inv.getProject() != null) {
            String label = inv.getInvoiceNumber() != null
                    ? "Qaimə: " + inv.getInvoiceNumber()
                    : "Qaimə #" + inv.getId();
            ProjectRevenue revenue = ProjectRevenue.builder()
                    .project(inv.getProject())
                    .key(label)
                    .value(inv.getAmount())
                    .date(inv.getInvoiceDate())
                    .build();
            projectRevenueRepository.save(revenue);
        }

        // Podratçı/İnvestor ödəməsi qaiməsi üçün layihə xərci + Kreditor moduluna sinxronlaşdır
        if (inv.getType() == InvoiceType.CONTRACTOR_EXPENSE || inv.getType() == InvoiceType.INVESTOR_EXPENSE) {
            if (inv.getProject() != null) {
                String payeeName = inv.getContractor() != null
                        ? inv.getContractor().getCompanyName()
                        : (inv.getCompanyName() != null ? inv.getCompanyName() : "Podratçı/İnvestor");
                String expenseLabel = (inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() + " — " : "") + payeeName;
                com.ces.erp.project.entity.ProjectExpense expense = com.ces.erp.project.entity.ProjectExpense.builder()
                        .project(inv.getProject())
                        .key(expenseLabel)
                        .value(inv.getAmount())
                        .date(inv.getInvoiceDate())
                        .build();
                projectExpenseRepository.save(expense);
            }
            payableService.syncPayableDebt(updated);
        }

        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "TƏSDİQLƏNDİ", "Qaimə mühasibatlıq tərəfindən təsdiqləndi");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    public InvoiceResponse returnToProject(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.SENT) {
            throw new BusinessException("Yalnız göndərilmiş qaimələr geri qaytarıla bilər");
        }
        inv.setStatus(InvoiceStatus.RETURNED);
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "GERİ QAYTARILDI", "Qaimə layihəyə geri qaytarıldı");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    public InvoiceResponse returnToDraft(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.RETURNED) {
            throw new BusinessException("Yalnız geri qaytarılmış qaimələr DRAFT-a qaytarıla bilər");
        }
        inv.setStatus(InvoiceStatus.DRAFT);
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "DRAFT-A QAYTARILDI",
                "Geri qaytarılmış qaimə redaktə üçün DRAFT-a çevrildi");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    public InvoiceResponse resubmit(Long id, InvoiceRequest req) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.RETURNED) {
            throw new BusinessException("Yalnız geri qaytarılmış qaimələr yenidən göndərilə bilər");
        }
        if (req.getInvoiceDate() != null)        inv.setInvoiceDate(req.getInvoiceDate());
        if (req.getNotes() != null)              inv.setNotes(req.getNotes().isBlank() ? null : req.getNotes().trim());
        if (req.getStandardDays() != null)       inv.setStandardDays(req.getStandardDays());
        if (req.getExtraDays() != null)          inv.setExtraDays(req.getExtraDays());
        if (req.getExtraHours() != null)         inv.setExtraHours(req.getExtraHours());
        if (req.getMonthlyRate() != null)        inv.setMonthlyRate(req.getMonthlyRate());
        if (req.getWorkingDaysInMonth() != null) inv.setWorkingDaysInMonth(req.getWorkingDaysInMonth());
        if (req.getWorkingHoursPerDay() != null) inv.setWorkingHoursPerDay(req.getWorkingHoursPerDay());
        if (req.getOvertimeRate() != null)       inv.setOvertimeRate(req.getOvertimeRate());
        BigDecimal recalc = calculateTimesheetAmount(req);
        if (recalc != null && recalc.compareTo(java.math.BigDecimal.ZERO) > 0) {
            inv.setAmount(recalc);
        }
        inv.setStatus(InvoiceStatus.SENT);
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "YENİDƏN GÖNDƏRİLDİ",
                "Geri qaytarılmış qaimə düzəliş edilib yenidən göndərildi");
        notificationService.info("Qaimə yenidən göndərildi",
                updated.getInvoiceNumber() + " nömrəli qaimə yenidən mühasibatlığa göndərildi", "ACCOUNTING");

        // Yenidən göndərildikdə də xərc qaiməsini yoxla / yarat (əgər əvvəl yaranmayıbsa)
        if (updated.getType() == InvoiceType.INCOME) {
            autoCreateExpenseInvoice(updated);
        }

        return InvoiceResponse.from(updated);
    }

    // ─── Avtomatik xərc qaiməsi ───────────────────────────────────────────────

    private void autoCreateExpenseInvoice(Invoice incomeInv) {
        if (incomeInv.getProject() == null || incomeInv.getProject().getRequest() == null) return;

        // Koordinator planını tap
        CoordinatorPlan plan = coordinatorPlanRepository
                .findByRequestId(incomeInv.getProject().getRequest().getId())
                .orElse(null);
        if (plan == null) return;

        BigDecimal dailyRate = plan.getContractorDailyRate();
        if (dailyRate == null || dailyRate.compareTo(BigDecimal.ZERO) == 0) return;

        // Texnikanı tap (planda seçilmiş, yoxsa sorğudan gələn)
        Equipment eq = plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : incomeInv.getProject().getRequest().getSelectedEquipment();
        if (eq == null) return;

        OwnershipType ownershipType = eq.getOwnershipType();
        if (ownershipType != OwnershipType.CONTRACTOR && ownershipType != OwnershipType.INVESTOR) return;

        // Məbləğ: (standart gün + əlavə gün) × gündəlik dərəcə
        int days = (incomeInv.getStandardDays() != null ? incomeInv.getStandardDays() : 0)
                 + (incomeInv.getExtraDays() != null ? incomeInv.getExtraDays() : 0);
        if (days == 0) return;

        // Layihə növünə görə gündəlik dərəcəni hesabla
        // MONTHLY: contractorDailyRate aylıq məbləğdir → gündəlik = aylıq / iş günü sayı
        // DAILY:   contractorDailyRate artıq günlük dərəcədir
        com.ces.erp.enums.ProjectType projectType = incomeInv.getProject().getRequest() != null
                ? incomeInv.getProject().getRequest().getProjectType()
                : null;

        BigDecimal perDayRate;
        if (projectType == com.ces.erp.enums.ProjectType.MONTHLY) {
            int workDays = incomeInv.getWorkingDaysInMonth() != null ? incomeInv.getWorkingDaysInMonth() : 26;
            perDayRate = dailyRate.divide(BigDecimal.valueOf(workDays), 6, RoundingMode.HALF_UP);
        } else {
            perDayRate = dailyRate;
        }

        BigDecimal daysAmount = perDayRate.multiply(BigDecimal.valueOf(days));

        // Əlavə saat hissəsi: (gündəlik / saat norması) × əlavə saat × əmsalı
        BigDecimal extraHoursAmount = BigDecimal.ZERO;
        if (incomeInv.getExtraHours() != null
                && incomeInv.getExtraHours().compareTo(BigDecimal.ZERO) > 0
                && incomeInv.getWorkingHoursPerDay() != null
                && incomeInv.getWorkingHoursPerDay() > 0) {
            BigDecimal hourlyRate = perDayRate.divide(
                    BigDecimal.valueOf(incomeInv.getWorkingHoursPerDay()), 6, RoundingMode.HALF_UP);
            BigDecimal overtimeRate = incomeInv.getOvertimeRate() != null
                    ? incomeInv.getOvertimeRate() : BigDecimal.ONE;
            extraHoursAmount = hourlyRate.multiply(incomeInv.getExtraHours()).multiply(overtimeRate);
        }

        BigDecimal amount = daysAmount.add(extraHoursAmount).setScale(2, RoundingMode.HALF_UP);

        // Eyni layihə + növ + dövr üçün artıq qaimə varmı?
        InvoiceType expType = ownershipType == OwnershipType.CONTRACTOR
                ? InvoiceType.CONTRACTOR_EXPENSE
                : InvoiceType.INVESTOR_EXPENSE;

        boolean alreadyExists = invoiceRepository
                .existsByProjectIdAndTypeAndPeriodMonthAndPeriodYearAndDeletedFalse(
                        incomeInv.getProject().getId(), expType,
                        incomeInv.getPeriodMonth(), incomeInv.getPeriodYear());
        if (alreadyExists) return;

        // Qaimə yarat (SENT — mühasibə görsənir, təsdiq gözləyir)
        Invoice.InvoiceBuilder builder = Invoice.builder()
                .type(expType)
                .status(InvoiceStatus.SENT)
                .amount(amount)
                .invoiceDate(incomeInv.getInvoiceDate())
                .project(incomeInv.getProject())
                .equipmentName(eq.getName())
                .periodMonth(incomeInv.getPeriodMonth())
                .periodYear(incomeInv.getPeriodYear())
                .standardDays(incomeInv.getStandardDays())
                .extraDays(incomeInv.getExtraDays())
                .notes("Gəlir qaiməsinə uyğun avtomatik yaradılmış ödəniş qaiməsi");

        if (ownershipType == OwnershipType.CONTRACTOR) {
            builder.contractor(eq.getOwnerContractor());
        } else {
            builder.companyName(eq.getOwnerInvestorName());
        }

        invoiceRepository.save(builder.build());
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private void validate(InvoiceRequest req, Long excludeId) {
        // Gəlir qaiməsi üçün layihə məcburidir
        if (req.getType() == InvoiceType.INCOME && req.getProjectId() == null) {
            throw new BusinessException("Gəlir qaiməsi üçün layihə seçilməlidir");
        }
        // Ödəmə və Xərc üçün layihə isteğe bağlıdır, podratçı da məcburi deyil
        if (req.getType() == InvoiceType.INCOME && req.getEtaxesId() != null && !req.getEtaxesId().isBlank()) {
            boolean exists = invoiceRepository.existsByEtaxesIdAndDeletedFalse(req.getEtaxesId());
            if (exists && excludeId == null) {
                throw new DuplicateResourceException("Bu ETaxes ID artıq mövcuddur: " + req.getEtaxesId());
            }
        }
    }

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Qaimə", id));
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

    private BigDecimal calculateTimesheetAmount(InvoiceRequest req) {
        if (req.getMonthlyRate() == null || req.getWorkingDaysInMonth() == null
                || req.getWorkingHoursPerDay() == null) return null;
        BigDecimal daily = req.getMonthlyRate()
                .divide(BigDecimal.valueOf(req.getWorkingDaysInMonth()), 6, RoundingMode.HALF_UP);
        BigDecimal std  = daily.multiply(BigDecimal.valueOf(req.getStandardDays() != null ? req.getStandardDays() : 0));
        BigDecimal extD = daily.multiply(BigDecimal.valueOf(req.getExtraDays() != null ? req.getExtraDays() : 0));
        BigDecimal rate = req.getOvertimeRate() != null ? req.getOvertimeRate() : BigDecimal.ONE;
        BigDecimal extH = req.getExtraHours() != null
                ? daily.divide(BigDecimal.valueOf(req.getWorkingHoursPerDay()), 6, RoundingMode.HALF_UP)
                       .multiply(req.getExtraHours())
                       .multiply(rate)
                : BigDecimal.ZERO;
        return std.add(extD).add(extH).setScale(2, RoundingMode.HALF_UP);
    }
}
