package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.AccountingSummaryResponse;
import com.ces.erp.accounting.dto.InvoiceRequest;
import com.ces.erp.accounting.dto.InvoiceResponse;
import com.ces.erp.accounting.dto.InvoiceTransportDto;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.entity.InvoiceTransport;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.accounting.repository.InvoiceTransportRepository;
import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.InvalidStatusTransitionException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.websocket.NotificationService;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.InvoiceStatus;
import com.ces.erp.project.entity.ProjectExpense;
import com.ces.erp.project.entity.ProjectRevenue;
import com.ces.erp.project.repository.ProjectExpenseRepository;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.project.repository.ProjectRevenueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService implements ApprovalHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTransportRepository invoiceTransportRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRevenueRepository projectRevenueRepository;
    private final ProjectExpenseRepository projectExpenseRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final CustomerRepository customerRepository;
    private final CoordinatorPlanRepository coordinatorPlanRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final FileStorageService fileStorageService;

    @Override public String getEntityType() { return "INVOICE"; }
    @Override public String getModuleCode()  { return "ACCOUNTING"; }
    @Override public String getLabel(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getAccountingId() != null) return inv.getAccountingId();
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
    public PagedResponse<InvoiceResponse> getAllPaged(int page, int size, String search, String type, String status, String types) {
        String q = (search != null && !search.isBlank()) ? search : null;
        List<InvoiceStatus> statuses = (status != null && !status.isBlank())
                ? List.of(InvoiceStatus.valueOf(status))
                : List.of(InvoiceStatus.values());
        var pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending().and(Sort.by("createdAt").descending()));

        // Çoxlu növ filtri (məs: CONTRACTOR_EXPENSE,INVESTOR_EXPENSE)
        if (types != null && !types.isBlank()) {
            List<InvoiceType> typeList = java.util.Arrays.stream(types.split(","))
                    .map(String::trim)
                    .map(InvoiceType::valueOf)
                    .toList();
            return PagedResponse.from(invoiceRepository.findAllFilteredByTypes(q, typeList, statuses, pageable), InvoiceResponse::from);
        }

        InvoiceType t = (type != null && !type.isBlank()) ? InvoiceType.valueOf(type) : null;
        return PagedResponse.from(invoiceRepository.findAllFiltered(q, t, statuses, pageable), InvoiceResponse::from);
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
            if (calc != null) inv.setAmount(calc.add(computeTransportTotal(req)));
        }

        if (req.getProjectId() != null) {
            var proj = projectRepository.findById(req.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Layihə", req.getProjectId()));
            inv.setProject(proj);
            // Texnikanı layihə → tələb → seçilmiş texnika zəncirindən ID ilə bağla (qazanc hesabatı üçün)
            if (proj.getRequest() != null && proj.getRequest().getSelectedEquipment() != null) {
                inv.setEquipment(proj.getRequest().getSelectedEquipment());
            }
        }
        if (req.getContractorId() != null) {
            inv.setContractor(contractorRepository.findById(req.getContractorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Podratçı", req.getContractorId())));
        }
        if (req.getCustomerId() != null) {
            inv.setCustomer(customerRepository.findByIdAndDeletedFalse(req.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Müştəri", req.getCustomerId())));
        }

        inv.setHasTransport(req.isHasTransport());

        // SENT statusunda mühasibatlığa göndərilirsə → avtomatik ID yarat
        if (inv.getStatus() == InvoiceStatus.SENT && inv.getAccountingId() == null) {
            inv.setAccountingId(generateAccountingId(java.time.LocalDate.now().getYear(), inv.getType()));
        }

        Invoice saved = invoiceRepository.save(inv);

        if (req.isHasTransport() && req.getTransports() != null) {
            saveTransports(saved, req.getTransports());
            saved = invoiceRepository.save(saved);
        }

        if (saved.getType() == InvoiceType.INCOME) {
            receivableService.syncInvoiceDebt(saved);
            // SENT statusunda yaradılıbsa (yəni layihə modulundan göndərilib) → podratçı/investor xərc qaiməsini avtomatik yarat
            if (saved.getStatus() == InvoiceStatus.SENT) {
                try {
                    autoCreateExpenseInvoice(saved);
                } catch (Exception e) {
                    log.error("[create] autoCreateExpenseInvoice xətası: {}", e.getMessage(), e);
                }
            }
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
            if (calc != null) inv.setAmount(calc.add(computeTransportTotal(req)));
        }

        inv.setProject(req.getProjectId() != null
                ? projectRepository.findById(req.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Layihə", req.getProjectId()))
                : null);
        inv.setContractor(req.getContractorId() != null
                ? contractorRepository.findById(req.getContractorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Podratçı", req.getContractorId()))
                : null);
        inv.setCustomer(req.getCustomerId() != null
                ? customerRepository.findByIdAndDeletedFalse(req.getCustomerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Müştəri", req.getCustomerId()))
                : null);

        inv.setHasTransport(req.isHasTransport());
        // Köhnə daşınmaları sil, yeniləri əlavə et
        inv.getTransports().clear();
        if (req.isHasTransport() && req.getTransports() != null) {
            saveTransports(inv, req.getTransports());
        }

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

        // Təsdiqlənmiş qaimədə yalnız qaimə nömrəsi və qeyd dəyişdirilə bilər
        if (inv.getStatus() == InvoiceStatus.APPROVED) {
            if (req.getInvoiceNumber() != null) inv.setInvoiceNumber(req.getInvoiceNumber().isBlank() ? null : req.getInvoiceNumber().trim());
            if (req.getNotes() != null) inv.setNotes(req.getNotes().isBlank() ? null : req.getNotes().trim());
            Invoice updated = invoiceRepository.save(inv);
            auditService.log("FAKTURA", updated.getId(), updated.getAccountingId(), "SAHƏ YENİLƏNDİ", "Mühasib əsl qaimə nömrəsini doldurdu");
            return InvoiceResponse.from(updated);
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
        // Status SENT-ə keçirsə → avtomatik ID yarat
        if (req.getStatus() != null) {
            if (req.getStatus() == InvoiceStatus.SENT && inv.getAccountingId() == null) {
                inv.setAccountingId(generateAccountingId(java.time.LocalDate.now().getYear(), inv.getType()));
            }
            inv.setStatus(req.getStatus());
        }
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getAccountingId(), "SAHƏ YENİLƏNDİ", "Mühasib sahələri doldurdu");

        // INCOME qaimə SENT-ə keçəndə (yəni mühasibatlığa göndəriləndə) → podratçı/investor xərc qaiməsini avtomatik yarat
        if (updated.getType() == InvoiceType.INCOME
                && updated.getStatus() == InvoiceStatus.SENT
                && prevStatus != InvoiceStatus.SENT) {
            try {
                autoCreateExpenseInvoice(updated);
            } catch (Exception e) {
                log.error("[patchFields] autoCreateExpenseInvoice xətası: {}", e.getMessage(), e);
            }
        }

        return InvoiceResponse.from(updated);
    }

    @Transactional
    @RequiresApproval(module = "ACCOUNTING", entityType = "INVOICE", isDelete = true)
    public void delete(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() == InvoiceStatus.APPROVED) {
            throw new BusinessException("Təsdiqlənmiş qaiməni silmək olmaz");
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

            // Daşınma məbləğlərini layihənin xərclərinə əlavə et
            if (inv.isHasTransport()) {
                inv.getTransports().stream()
                        .filter(t -> !t.isDeleted())
                        .forEach(t -> {
                            String transportLabel = "Daşınma: " + t.getTransportDirection();
                            ProjectExpense expense = ProjectExpense.builder()
                                    .project(inv.getProject())
                                    .key(transportLabel)
                                    .value(t.getTransportAmount())
                                    .date(t.getTransportDate())
                                    .build();
                            projectExpenseRepository.save(expense);
                        });
            }
        }

        // Podratçı / İnvestor xərc qaiməsini avtomatik yarat
        if (updated.getType() == InvoiceType.INCOME && updated.getProject() != null) {
            try {
                autoCreateExpenseInvoice(updated);
            } catch (Exception e) {
                log.error("[approve] autoCreateExpenseInvoice xətası: {}", e.getMessage(), e);
            }
        }

        // CONTRACTOR/INVESTOR_EXPENSE təsdiqlənəndə layihə xərcinə əlavə et + Kreditor sinxronlaşdır
        if (inv.getType() == InvoiceType.CONTRACTOR_EXPENSE || inv.getType() == InvoiceType.INVESTOR_EXPENSE) {
            if (inv.getProject() != null) {
                String payeeName = inv.getContractor() != null
                        ? inv.getContractor().getCompanyName()
                        : (inv.getCompanyName() != null ? inv.getCompanyName() : "Podratçı/İnvestor");
                String expenseLabel = (inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() + " — " : "") + payeeName;
                ProjectExpense expense = ProjectExpense.builder()
                        .project(inv.getProject())
                        .key(expenseLabel)
                        .value(inv.getAmount())
                        .date(inv.getInvoiceDate())
                        .build();
                projectExpenseRepository.save(expense);
            }
            payableService.syncPayableDebt(updated);
        }

        // Debitor cədvəlinə sync et (gəlir qaiməsi təsdiqləndi)
        if (updated.getType() == InvoiceType.INCOME) {
            receivableService.syncInvoiceDebt(updated);
        }

        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "TƏSDİQLƏNDİ", "Qaimə mühasibatlıq tərəfindən təsdiqləndi");
        return InvoiceResponse.from(updated);
    }

    private void autoCreateExpenseInvoice(Invoice incomeInvoice) {
        log.info("[autoCreateExpenseInvoice] Başladı: incomeInvoiceId={}, type={}, status={}",
                incomeInvoice.getId(), incomeInvoice.getType(), incomeInvoice.getStatus());

        var project = incomeInvoice.getProject();
        if (project == null || project.getRequest() == null) {
            log.warn("[autoCreateExpenseInvoice] Layihə və ya request boşdur — keçirik");
            return;
        }

        // Artıq bu gəlir qaiməsinə bağlı xərc qaiməsi varsa — yenidən yaratma
        if (invoiceRepository.existsBySourceInvoiceIdAndDeletedFalse(incomeInvoice.getId())) {
            log.info("[autoCreateExpenseInvoice] Artıq bu gəlir qaiməsi üçün xərc qaiməsi mövcuddur — keçirik");
            return;
        }

        var plan = coordinatorPlanRepository.findByRequestId(project.getRequest().getId()).orElse(null);
        log.info("[autoCreateExpenseInvoice] Plan tapıldı: {}", plan != null ? "BƏLİ (id=" + plan.getId() + ")" : "XEYR");

        Equipment eq = plan != null && plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : project.getRequest().getSelectedEquipment();
        if (eq == null) {
            log.warn("[autoCreateExpenseInvoice] Texnika tapılmadı (plan və request hər ikisində boş) — keçirik");
            return;
        }
        log.info("[autoCreateExpenseInvoice] Texnika: {} — Sahiblik: {}", eq.getName(), eq.getOwnershipType());

        if (eq.getOwnershipType() == OwnershipType.COMPANY) {
            log.info("[autoCreateExpenseInvoice] Texnika şirkətə məxsusdur — xərc qaiməsi yaradılmır");
            return;
        }

        // Məbləği gəlir qaiməsinin gün/saat məlumatlarına və planın contractorDailyRate-inə görə hesabla
        // (Frontend ilə eyni düstur: perDay × (std+ext gün) + (perDay / saat) × əlavə saat)
        BigDecimal expenseAmount = BigDecimal.ZERO;
        if (plan != null) {
            boolean isDaily = project.getRequest().getProjectType() == ProjectType.DAILY;
            BigDecimal contractorRate = plan.getContractorDailyRate();
            if (contractorRate != null && contractorRate.compareTo(BigDecimal.ZERO) > 0) {
                int workDaysInMonth = isDaily ? 1
                        : (incomeInvoice.getWorkingDaysInMonth() != null && incomeInvoice.getWorkingDaysInMonth() > 0
                                ? incomeInvoice.getWorkingDaysInMonth() : 26);
                int workHoursPerDay = incomeInvoice.getWorkingHoursPerDay() != null && incomeInvoice.getWorkingHoursPerDay() > 0
                        ? incomeInvoice.getWorkingHoursPerDay() : 9;
                int stdDays = incomeInvoice.getStandardDays() != null ? incomeInvoice.getStandardDays() : 0;
                int extDays = incomeInvoice.getExtraDays() != null ? incomeInvoice.getExtraDays() : 0;
                BigDecimal extHours = incomeInvoice.getExtraHours() != null ? incomeInvoice.getExtraHours() : BigDecimal.ZERO;

                BigDecimal perDay = contractorRate.divide(BigDecimal.valueOf(workDaysInMonth), 6, RoundingMode.HALF_UP);
                BigDecimal daysAmt = perDay.multiply(BigDecimal.valueOf((long) stdDays + extDays));
                BigDecimal extHAmt = perDay
                        .divide(BigDecimal.valueOf(workHoursPerDay), 6, RoundingMode.HALF_UP)
                        .multiply(extHours);
                expenseAmount = daysAmt.add(extHAmt).setScale(2, RoundingMode.HALF_UP);
            } else if (plan.getContractorPayment() != null
                    && plan.getContractorPayment().compareTo(BigDecimal.ZERO) > 0) {
                // Fallback: dailyRate yoxdursa cəmi ödənişdən istifadə et
                expenseAmount = plan.getContractorPayment();
            } else if (plan.getEquipmentPrice() != null
                    && plan.getEquipmentPrice().compareTo(BigDecimal.ZERO) > 0) {
                // Fallback: günlük dərəcə/ödəniş yoxdursa "texnika xərci" (equipmentPrice)
                expenseAmount = plan.getEquipmentPrice();
            }
        }

        String incomeRef = incomeInvoice.getInvoiceNumber() != null
                ? incomeInvoice.getInvoiceNumber()
                : (incomeInvoice.getAccountingId() != null ? incomeInvoice.getAccountingId() : "#" + incomeInvoice.getId());

        Invoice.InvoiceBuilder builder = Invoice.builder()
                .status(InvoiceStatus.SENT)
                .amount(expenseAmount)
                .invoiceDate(incomeInvoice.getInvoiceDate())
                .project(project)
                .equipment(eq)
                .equipmentName(eq.getName())
                .periodMonth(incomeInvoice.getPeriodMonth())
                .periodYear(incomeInvoice.getPeriodYear())
                .sourceInvoiceId(incomeInvoice.getId())
                .notes("\"" + incomeRef + "\" gəlir qaiməsindən avtomatik yaradıldı");

        if (eq.getOwnershipType() == OwnershipType.CONTRACTOR) {
            builder.type(InvoiceType.CONTRACTOR_EXPENSE)
                   .contractor(eq.getOwnerContractor());
        } else {
            builder.type(InvoiceType.INVESTOR_EXPENSE)
                   .companyName(eq.getOwnerInvestorName());
            // İnvestor FK-nı VÖEN ilə bağla — portal dashboard/invoices i.investor.id ilə süzür
            if (eq.getOwnerInvestorVoen() != null) {
                investorRepository.findByVoenAndDeletedFalse(eq.getOwnerInvestorVoen())
                        .ifPresent(builder::investor);
            }
        }

        Invoice expense = builder.build();
        expense.setAccountingId(generateAccountingId(incomeInvoice.getInvoiceDate().getYear(), expense.getType()));
        Invoice saved = invoiceRepository.save(expense);
        log.info("[autoCreateExpenseInvoice] Xərc qaiməsi yaradıldı: id={}, type={}, amount={}, accountingId={}",
                saved.getId(), saved.getType(), saved.getAmount(), saved.getAccountingId());
    }

    @Transactional
    public InvoiceResponse returnToProject(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.SENT) {
            throw new BusinessException("Yalnız göndərilmiş qaimələr geri qaytarıla bilər");
        }
        inv.setStatus(InvoiceStatus.RETURNED);
        Invoice updated = invoiceRepository.save(inv);

        // Bağlı xərc qaimələrini (podratçı/investor) sil
        List<Invoice> linked = invoiceRepository.findAllBySourceInvoiceIdAndDeletedFalse(id);
        for (Invoice exp : linked) {
            exp.softDelete();
            invoiceRepository.save(exp);
            auditService.log("FAKTURA", exp.getId(), exp.getAccountingId(), "SİLİNDİ", "Ana qaimə geri qaytarıldığı üçün avtomatik silindi");
        }

        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "GERİ QAYTARILDI", "Qaimə layihəyə geri qaytarıldı");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    public InvoiceResponse returnToDraft(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.RETURNED) {
            throw new BusinessException("Yalnız geri qaytarılmış qaimələr DRAFT-a çevrilə bilər");
        }
        inv.setStatus(InvoiceStatus.DRAFT);
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getInvoiceNumber(), "DRAFT-A ÇEVRİLDİ", "Qaimə yenidən redaktə üçün DRAFT-a qaytarıldı");
        return InvoiceResponse.from(updated);
    }

    @Transactional
    public InvoiceResponse resubmit(Long id, InvoiceRequest req) {
        Invoice inv = findOrThrow(id);
        if (inv.getStatus() != InvoiceStatus.RETURNED) {
            throw new BusinessException("Yalnız geri qaytarılmış qaimələr yenidən göndərilə bilər");
        }
        // Yalnız request-də verilən sahələri yenilə (qalanları olduğu kimi saxla)
        if (req.getAmount() != null)              inv.setAmount(req.getAmount());
        if (req.getInvoiceDate() != null)         inv.setInvoiceDate(req.getInvoiceDate());
        if (req.getEtaxesId() != null)            inv.setEtaxesId(req.getEtaxesId().isBlank() ? null : req.getEtaxesId().trim());
        if (req.getEquipmentName() != null)       inv.setEquipmentName(req.getEquipmentName());
        if (req.getCompanyName() != null)         inv.setCompanyName(req.getCompanyName());
        if (req.getServiceDescription() != null)  inv.setServiceDescription(req.getServiceDescription());
        if (req.getNotes() != null)               inv.setNotes(req.getNotes().isBlank() ? null : req.getNotes().trim());
        if (req.getStandardDays() != null)        inv.setStandardDays(req.getStandardDays());
        if (req.getExtraDays() != null)           inv.setExtraDays(req.getExtraDays());
        if (req.getExtraHours() != null)          inv.setExtraHours(req.getExtraHours());
        if (req.getMonthlyRate() != null)         inv.setMonthlyRate(req.getMonthlyRate());
        if (req.getWorkingDaysInMonth() != null)  inv.setWorkingDaysInMonth(req.getWorkingDaysInMonth());
        if (req.getWorkingHoursPerDay() != null)  inv.setWorkingHoursPerDay(req.getWorkingHoursPerDay());
        if (req.getOvertimeRate() != null)        inv.setOvertimeRate(req.getOvertimeRate());

        // Vaxt cədvəlinə görə məbləği yenidən hesabla (məcburi sahələr varsa)
        BigDecimal recalc = calculateTimesheetAmount(req);
        if (recalc != null && recalc.compareTo(BigDecimal.ZERO) > 0) {
            inv.setAmount(recalc.add(computeTransportTotal(req)));
        }

        // Yalnız tam paket göndərildiyi halda daşınmaları sıfırla (yoxsa mövcud olanları qoru)
        if (req.getTransports() != null) {
            inv.setHasTransport(req.isHasTransport());
            inv.getTransports().clear();
            if (req.isHasTransport()) {
                saveTransports(inv, req.getTransports());
            }
        }

        // Səhv konfiqurasiyaya qarşı son qoruma — amount null-a düşməsin
        if (inv.getAmount() == null) {
            throw new BusinessException("Qaimə məbləği boş ola bilməz. Standart günlər və aylıq dərəcə daxil edin.");
        }

        // Əgər bu qaimənin hələ accountingId-si yoxdursa yarat (ilk dəfə resubmit olan köhnə qaimələr üçün)
        if (inv.getAccountingId() == null) {
            inv.setAccountingId(generateAccountingId(java.time.LocalDate.now().getYear(), inv.getType()));
        }
        inv.setStatus(InvoiceStatus.SENT);
        Invoice updated = invoiceRepository.save(inv);
        auditService.log("FAKTURA", updated.getId(), updated.getAccountingId(), "YENİDƏN GÖNDƏRİLDİ", "Düzəliş edilmiş qaimə yenidən mühasibatlığa göndərildi");

        // Yenidən göndərildikdə də xərc qaiməsini yoxla / yarat (əgər əvvəl yaranmayıbsa)
        if (updated.getType() == InvoiceType.INCOME) {
            try {
                autoCreateExpenseInvoice(updated);
            } catch (Exception e) {
                log.error("[resubmit] autoCreateExpenseInvoice xətası: {}", e.getMessage(), e);
            }
        }

        return InvoiceResponse.from(updated);
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

    private void saveTransports(Invoice invoice, List<InvoiceTransportDto> dtos) {
        if (dtos == null) return;
        List<InvoiceTransport> transports = new ArrayList<>();
        for (InvoiceTransportDto dto : dtos) {
            InvoiceTransport t = InvoiceTransport.builder()
                    .invoice(invoice)
                    .transportDate(dto.getTransportDate())
                    .transportDirection(dto.getTransportDirection())
                    .transportAmount(dto.getTransportAmount())
                    .build();
            transports.add(t);
        }
        invoice.getTransports().addAll(transports);
    }

    @Transactional
    public InvoiceResponse uploadAkt(Long id, MultipartFile file) {
        Invoice inv = findOrThrow(id);
        if (inv.getAktFilePath() != null) {
            fileStorageService.delete(inv.getAktFilePath());
        }
        String path = fileStorageService.store(file, "invoice-akt");
        inv.setAktFilePath(path);
        inv.setAktFileName(file.getOriginalFilename());
        Invoice saved = invoiceRepository.save(inv);
        auditService.log("FAKTURA", saved.getId(), saved.getAccountingId(), "AKT YÜKLƏNDİ", "Təhvil-Təslim Aktı yükləndi: " + file.getOriginalFilename());
        return InvoiceResponse.from(saved);
    }

    public Path resolveAktPath(Long id) {
        Invoice inv = findOrThrow(id);
        if (inv.getAktFilePath() == null) {
            throw new ResourceNotFoundException("Bu qaimə üçün Akt faylı tapılmadı");
        }
        return fileStorageService.resolve(inv.getAktFilePath());
    }

    private synchronized String generateAccountingId(int year, InvoiceType type) {
        // INCOME və İNVESTOR ödəmələri → "INV-"; podratçı/şirkət xərcləri → "POD-"
        boolean inv = (type == InvoiceType.INCOME || type == InvoiceType.INVESTOR_EXPENSE);
        String prefix = (inv ? "INV-" : "POD-") + year + "-";
        java.util.Optional<String> maxId = invoiceRepository.findMaxAccountingIdForYear(prefix);
        int seq = 1;
        if (maxId.isPresent() && maxId.get() != null) {
            try {
                seq = Integer.parseInt(maxId.get().substring(prefix.length())) + 1;
            } catch (Exception ignored) {}
        }
        return prefix + String.format("%05d", seq);
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

    private BigDecimal computeTransportTotal(InvoiceRequest req) {
        if (!req.isHasTransport() || req.getTransports() == null) return BigDecimal.ZERO;
        return req.getTransports().stream()
                .map(t -> t.getTransportAmount() != null ? t.getTransportAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
