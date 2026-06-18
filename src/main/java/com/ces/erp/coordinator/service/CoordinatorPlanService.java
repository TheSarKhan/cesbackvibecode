package com.ces.erp.coordinator.service;

import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.common.websocket.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.coordinator.dto.CoordinatorPlanRequest;
import com.ces.erp.coordinator.dto.CoordinatorPlanResponse;
import com.ces.erp.coordinator.entity.CoordinatorDocument;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.entity.CoordinatorPlanItem;
import com.ces.erp.coordinator.repository.CoordinatorDocumentRepository;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.config.repository.ConfigItemRepository;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.entity.EquipmentDocument;
import com.ces.erp.garage.repository.EquipmentDocumentRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.projectmanager.entity.PartyType;
import com.ces.erp.projectmanager.entity.RequestShortlist;
import com.ces.erp.projectmanager.entity.ShortlistItem;
import com.ces.erp.projectmanager.repository.RequestShortlistRepository;
import com.ces.erp.projectmanager.repository.ShortlistItemRepository;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoordinatorPlanService implements ApprovalHandler {

    private final TechRequestRepository requestRepository;
    private final CoordinatorPlanRepository planRepository;
    private final com.ces.erp.coordinator.repository.CoordinatorPlanItemRepository planItemRepository;
    private final CoordinatorDocumentRepository documentRepository;
    private final EquipmentRepository equipmentRepository;
    private final com.ces.erp.garage.service.EquipmentService equipmentService;
    private final EquipmentDocumentRepository equipmentDocumentRepository;
    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final PendingOperationRepository pendingOperationRepository;
    private final ConfigItemRepository configItemRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final ShortlistItemRepository shortlistItemRepository;
    private final RequestShortlistRepository requestShortlistRepository;
    private final com.ces.erp.request.repository.RequestStatusLogRepository statusLogRepository;
    private final com.ces.erp.common.audit.AuditService auditService;
    private final com.ces.erp.request.service.RequestTransitionService transitionService;
    private final com.ces.erp.request.repository.RequestDocumentRepository requestDocumentRepository;

    @Override public String getEntityType() { return "COORDINATOR_SUBMIT"; }
    @Override public String getModuleCode()  { return "COORDINATOR"; }
    @Override public String getLabel(Long id) {
        return requestRepository.findByIdAndDeletedFalse(id)
                .map(TechRequest::getRequestCode).orElse("Sorğu #" + id);
    }
    @Override public Object getSnapshot(Long id) {
        return planRepository.findByRequestId(id)
                .map(CoordinatorPlanResponse::from)
                .orElse(null);
    }
    @Override public void applyEdit(Long id, String json) {
        ApprovalContext.setApplying(true);
        try { submitPlan(id); } finally { ApprovalContext.clear(); }
    }
    @Override public void applyDelete(Long id) { /* istifadə edilmir */ }

    // Yeni flowda koordinator iki mərhələdə işləyir:
    // Mərhələ A: COORDINATOR_NEGOTIATING (danışıq), COORDINATOR_PROPOSED (geri PM-ə)
    // Mərhələ B: EXECUTION_READY, OPERATOR_ASSIGNED, EQUIPMENT_DISPATCHED, DELIVERED (icra)
    private static final List<RequestStatus> COORDINATOR_STATUSES = List.of(
            RequestStatus.COORDINATOR_NEGOTIATING,
            RequestStatus.COORDINATOR_PROPOSED,
            RequestStatus.EXECUTION_READY,
            RequestStatus.OPERATOR_ASSIGNED,
            RequestStatus.EQUIPMENT_DISPATCHED,
            RequestStatus.DELIVERED,
            RequestStatus.REJECTED
    );

    @Transactional(readOnly = true)
    public Map<RequestStatus, Long> getStats() {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : COORDINATOR_STATUSES) result.put(s, 0L);
        requestRepository.countGroupedByStatusIn(COORDINATOR_STATUSES)
                .forEach(row -> result.put(row.getStatus(), row.getCnt()));
        return result;
    }

    public List<CoordinatorPlanResponse> getRequests() {
        return requestRepository.findAllByStatusInAndDeletedFalse(COORDINATOR_STATUSES).stream()
                .map(r -> {
                    CoordinatorPlanResponse resp = planRepository.findByRequestId(r.getId())
                            .map(CoordinatorPlanResponse::from)
                            .orElseGet(() -> CoordinatorPlanResponse.fromRequest(r));
                    resp.setHasPendingSubmit(pendingOperationRepository
                            .existsByEntityTypeAndEntityIdAndStatusAndDeletedFalse(
                                    "COORDINATOR_SUBMIT", r.getId(), OperationStatus.PENDING));
                    return resp;
                })
                .toList();
    }

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of(
            "createdAt", "companyName", "requestCode", "status", "region", "projectType", "dayCount");

    @Transactional(readOnly = true)
    public PagedResponse<CoordinatorPlanResponse> getRequestsPaged(int page, int size, String search, String status,
                                                                     String sortBy, String sortDir) {
        String q = (search != null && !search.isBlank()) ? search : null;
        RequestStatus s = null;
        if (status != null && !status.isBlank()) {
            try { s = RequestStatus.valueOf(status); } catch (IllegalArgumentException ignored) { }
        }
        // If a specific status is requested but it's not a coordinator status, return empty
        if (s != null && !COORDINATOR_STATUSES.contains(s)) {
            s = null;
        }
        String field = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(field).ascending() : Sort.by(field).descending();
        var pageable = PageRequest.of(page, size, sort);
        var result = requestRepository.findAllCoordinatorFiltered(q, s, pageable);
        return PagedResponse.from(result, r -> {
            CoordinatorPlanResponse resp = planRepository.findByRequestId(r.getId())
                    .map(CoordinatorPlanResponse::from)
                    .orElseGet(() -> CoordinatorPlanResponse.fromRequest(r));
            resp.setHasPendingSubmit(pendingOperationRepository
                    .existsByEntityTypeAndEntityIdAndStatusAndDeletedFalse(
                            "COORDINATOR_SUBMIT", r.getId(), OperationStatus.PENDING));
            return resp;
        });
    }

    public CoordinatorPlanResponse getPlan(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        CoordinatorPlanResponse resp = planRepository.findByRequestId(requestId)
                .map(CoordinatorPlanResponse::from)
                .orElseGet(() -> CoordinatorPlanResponse.fromRequest(request));
        resp.setShortlistItems(loadShortlistRows(requestId));
        attachAgreementDocuments(resp, requestId);
        return resp;
    }

    /** Hər texnika xəttinə müqavilə sənədlərini (müştəri + sahib) bağla — koordinator oxu-rejimi. */
    private void attachAgreementDocuments(CoordinatorPlanResponse resp, Long requestId) {
        if (resp.getItems() == null || resp.getItems().isEmpty()) return;
        var docs = requestDocumentRepository.findAllByRequestIdAndDeletedFalse(requestId);
        if (docs.isEmpty()) return;
        for (var item : resp.getItems()) {
            var agDocs = docs.stream()
                    // xəttə bağlı sənəd VƏ YA sorğu səviyyəli (köhnə) sənəd
                    .filter(d -> d.getPlanItem() == null
                            || (d.getPlanItem().getId().equals(item.getId())))
                    .map(d -> CoordinatorPlanResponse.AgreementDocDto.builder()
                            .id(d.getId())
                            .docType(d.getDocType() != null ? d.getDocType().name() : null)
                            .fileName(d.getFileName())
                            .build())
                    .toList();
            item.setAgreementDocuments(agDocs);
        }
    }

    private List<CoordinatorPlanResponse.ShortlistRowDto> loadShortlistRows(Long requestId) {
        var items = shortlistItemRepository.findAllByShortlist_Request_IdAndDeletedFalseOrderByRankAscIdAsc(requestId);
        return items.stream().map(it -> {
            var b = CoordinatorPlanResponse.ShortlistRowDto.builder()
                    .id(it.getId())
                    .partyType(it.getPartyType() != null ? it.getPartyType().name() : null)
                    .negotiatedPrice(it.getNegotiatedPrice())
                    .rank(it.getRank())
                    .notes(it.getNotes());
            if (it.getContractor() != null) {
                var c = it.getContractor();
                b.contractorId(c.getId())
                 .contractorName(c.getCompanyName())
                 .contractorVoen(c.getVoen())
                 .contractorPhone(c.getPhone())
                 .contractorContactPerson(c.getContactPerson())
                 .contractorAddress(c.getAddress());
            }
            if (it.getInvestor() != null) {
                var iv = it.getInvestor();
                b.investorId(iv.getId())
                 .investorName(iv.getCompanyName())
                 .investorVoen(iv.getVoen())
                 .investorPhone(iv.getContactPhone())
                 .investorContactPerson(iv.getContactPerson())
                 .investorAddress(iv.getAddress());
            }
            if (it.getEquipment() != null) {
                var eq = it.getEquipment();
                b.equipmentId(eq.getId())
                 .equipmentName(eq.getName())
                 .equipmentCode(eq.getEquipmentCode())
                 .equipmentType(eq.getType())
                 .equipmentBrand(eq.getBrand())
                 .equipmentModel(eq.getModel())
                 .equipmentYear(eq.getManufactureYear())
                 .equipmentPlateNumber(eq.getPlateNumber())
                 .equipmentOwnership(eq.getOwnershipType() != null ? eq.getOwnershipType().name() : null);
            }
            return b.build();
        }).toList();
    }

    @Transactional
    public CoordinatorPlanResponse savePlan(Long requestId, CoordinatorPlanRequest req, Long userId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (!COORDINATOR_STATUSES.contains(request.getStatus())) {
            throw new BusinessException("Bu sorğu koordinator üçün uyğun deyil");
        }

        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseGet(() -> CoordinatorPlan.builder().request(request).build());

        // Operator yalnız icra fazasında təyin edilir; danışıq fazasında ignore et
        if (req.getOperatorId() != null && request.getStatus() != RequestStatus.COORDINATOR_NEGOTIATING) {
            var operator = operatorRepository.findByIdActive(req.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator", req.getOperatorId()));
            boolean busy = planRepository.isOperatorBusyInOtherProject(
                    req.getOperatorId(), requestId,
                    List.of(ProjectStatus.PENDING, ProjectStatus.ACTIVE));
            if (busy) {
                throw new BusinessException("Bu operator artıq başqa aktiv layihəyə təyin edilib");
            }
            plan.setOperator(operator);
        }

        // Shortlist sətirləri — update (mövcud itemId) və create (itemId null)
        if (req.getShortlistRows() != null && !req.getShortlistRows().isEmpty()) {
            // Koordinator yalnız danışıq mərhələsində yeni sətr əlavə edə bilər
            boolean canAddNew = request.getStatus() == RequestStatus.COORDINATOR_NEGOTIATING;

            // Shortlist konteynerini al və ya yarat (yeni sətr üçün lazımdır)
            RequestShortlist sl = requestShortlistRepository.findByRequestIdAndDeletedFalse(requestId)
                    .orElseGet(() -> requestShortlistRepository.save(
                            RequestShortlist.builder().request(request).build()));

            for (var row : req.getShortlistRows()) {
                if (row.getItemId() != null) {
                    // UPDATE — mövcud sətir
                    shortlistItemRepository.findById(row.getItemId()).ifPresent(item -> {
                        if (row.getNegotiatedPrice() != null) item.setNegotiatedPrice(row.getNegotiatedPrice());
                        if (row.getRank() != null) item.setRank(row.getRank());
                        if (row.getNotes() != null) item.setNotes(row.getNotes());
                        shortlistItemRepository.save(item);
                    });
                } else if (canAddNew && row.getPartyType() != null) {
                    // CREATE — yeni sətir
                    PartyType pt;
                    try {
                        pt = PartyType.valueOf(row.getPartyType());
                    } catch (IllegalArgumentException e) {
                        throw new BusinessException("Naməlum tərəf tipi: " + row.getPartyType());
                    }
                    Contractor contractor = null;
                    Investor investor = null;
                    if (pt == PartyType.CONTRACTOR) {
                        if (row.getContractorId() == null) {
                            throw new BusinessException("Podratçı sətrində Podratçı seçilməlidir");
                        }
                        contractor = contractorRepository.findById(row.getContractorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Podratçı", row.getContractorId()));
                    } else if (pt == PartyType.INVESTOR) {
                        if (row.getInvestorId() == null) {
                            throw new BusinessException("Investor sətrində Investor seçilməlidir");
                        }
                        investor = investorRepository.findById(row.getInvestorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Investor", row.getInvestorId()));
                    }
                    Equipment eq = row.getEquipmentId() != null
                            ? equipmentRepository.findById(row.getEquipmentId())
                                .orElseThrow(() -> new ResourceNotFoundException("Texnika", row.getEquipmentId()))
                            : null;
                    ShortlistItem item = ShortlistItem.builder()
                            .shortlist(sl)
                            .partyType(pt)
                            .contractor(contractor)
                            .investor(investor)
                            .equipment(eq)
                            .negotiatedPrice(row.getNegotiatedPrice())
                            .rank(row.getRank())
                            .notes(row.getNotes())
                            .build();
                    shortlistItemRepository.save(item);
                }
            }
        }

        // Qalib shortlist sətri (texnika avtomatik ondan götürülür)
        if (req.getWinnerItemId() != null) {
            ShortlistItem winner = shortlistItemRepository.findById(req.getWinnerItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shortlist sətri", req.getWinnerItemId()));
            plan.setWinnerItem(winner);
            // Texnikanı qalib sətrdən sinxronlaşdır
            if (winner.getEquipment() != null) {
                plan.setSelectedEquipment(winner.getEquipment());
            }
        }

        plan.setDayCount(req.getDayCount());
        plan.setEquipmentPrice(req.getEquipmentPrice());
        plan.setCustomerEquipmentPrice(req.getCustomerEquipmentPrice());

        // Podratçı/İnvestor ödənişi: aylıq → sabit dərəcə, günlük → dərəcə × gün sayı
        BigDecimal dailyRate = req.getContractorDailyRate() != null ? req.getContractorDailyRate() : BigDecimal.ZERO;
        plan.setContractorDailyRate(dailyRate);
        if (dailyRate.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getProjectType() == ProjectType.MONTHLY) {
                plan.setContractorPayment(dailyRate);
            } else if (req.getDayCount() != null && req.getDayCount() > 0) {
                plan.setContractorPayment(dailyRate.multiply(BigDecimal.valueOf(req.getDayCount())));
            } else {
                plan.setContractorPayment(BigDecimal.ZERO);
            }
        } else {
            plan.setContractorPayment(BigDecimal.ZERO);
        }

        plan.setOperatorPayment(req.getOperatorPayment());
        plan.setTransportationPrice(req.getTransportationPrice());
        if (req.getTransportContractorId() != null) {
            plan.setTransportContractor(contractorRepository.findById(req.getTransportContractorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Daşınma podratçısı", req.getTransportContractorId())));
        } else {
            plan.setTransportContractor(null);
        }
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        if (req.getSafetyEquipmentIds() != null) {
            plan.getSafetyEquipment().clear();
            plan.getSafetyEquipment().addAll(configItemRepository.findAllById(req.getSafetyEquipmentIds()));
        }
        plan.setNotes(req.getNotes());

        CoordinatorPlan saved = planRepository.save(plan);

        // ─── Yeni model: çoxlu texnika xətləri (legacy körpü yoxdur — downstream item-əsaslı) ───
        if (req.getItems() != null) {
            reconcilePlanItems(saved, request, req.getItems());
        }

        CoordinatorPlanResponse resp = CoordinatorPlanResponse.from(saved);
        resp.setShortlistItems(loadShortlistRows(requestId));
        return resp;
    }

    /** Seçilmiş texnika xətlərini upsert et, seçimdən çıxanları soft-delete et. */
    private void reconcilePlanItems(CoordinatorPlan plan, TechRequest request,
                                    List<CoordinatorPlanRequest.PlanItemInput> inputs) {
        List<CoordinatorPlanItem> existing = planItemRepository.findAllByPlanIdAndDeletedFalse(plan.getId());
        java.util.Set<Long> incomingIds = inputs.stream()
                .map(CoordinatorPlanRequest.PlanItemInput::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        for (CoordinatorPlanItem old : existing) {
            if (!incomingIds.contains(old.getId())) {
                old.softDelete();
                planItemRepository.save(old);
            }
        }

        for (CoordinatorPlanRequest.PlanItemInput in : inputs) {
            CoordinatorPlanItem item = in.getId() != null
                    ? planItemRepository.findById(in.getId()).orElse(null)
                    : null;
            if (item == null) {
                item = CoordinatorPlanItem.builder().plan(plan).build();
            }
            // Sahib + texnika mənbə shortlist sətrindən sinxronlaşdırılır
            if (in.getShortlistItemId() != null) {
                ShortlistItem sit = shortlistItemRepository.findById(in.getShortlistItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Shortlist sətri", in.getShortlistItemId()));
                item.setShortlistItem(sit);
                item.setPartyType(sit.getPartyType());
                item.setContractor(sit.getContractor());
                item.setInvestor(sit.getInvestor());
                item.setEquipment(sit.getEquipment());
            }
            item.setEquipmentPrice(in.getEquipmentPrice());
            item.setCustomerEquipmentPrice(in.getCustomerEquipmentPrice());
            item.setTransportationPrice(in.getTransportationPrice());
            item.setDayCount(in.getDayCount() != null ? in.getDayCount() : request.getDayCount());
            item.setStartDate(in.getStartDate());
            item.setEndDate(in.getEndDate());
            planItemRepository.save(item);
        }
    }

    public void validateBeforeSubmit(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.COORDINATOR_NEGOTIATING) {
            throw new BusinessException("Plan yalnız COORDINATOR_NEGOTIATING statusunda göndərilə bilər");
        }
        CoordinatorPlan existing = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Əvvəlcə koordinator planını doldurun"));

        // Yeni model — çoxlu texnika xətləri varsa onları yoxla
        List<CoordinatorPlanItem> items = planItemRepository.findAllByPlanIdAndDeletedFalse(existing.getId());
        if (!items.isEmpty()) {
            for (CoordinatorPlanItem it : items) {
                if (it.getEquipment() == null) {
                    throw new BusinessException("Seçilmiş hər sətrdə texnika olmalıdır");
                }
                boolean isCompany = it.getPartyType() == com.ces.erp.projectmanager.entity.PartyType.COMPANY;
                if (!isCompany && (it.getEquipmentPrice() == null
                        || it.getEquipmentPrice().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
                    throw new BusinessException("Hər podratçı/investor texnikası üçün ödəniləcək qiymət daxil edilməlidir");
                }
                if (it.getCustomerEquipmentPrice() == null
                        || it.getCustomerEquipmentPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Hər texnika üçün müştəriyə təklif qiyməti daxil edilməlidir");
                }
            }
            return;
        }

        // Legacy — tək qalib model
        if (existing.getWinnerItem() == null) {
            throw new BusinessException("Shortlist-dən ən az bir texnika seçilməlidir");
        }
        if (existing.getWinnerItem().getEquipment() == null) {
            throw new BusinessException("Qalib sətrdə texnika qeyd edilməlidir");
        }
        boolean winnerIsCompany = existing.getWinnerItem().getPartyType() == com.ces.erp.projectmanager.entity.PartyType.COMPANY;
        if (!winnerIsCompany) {
            if (existing.getEquipmentPrice() == null || existing.getEquipmentPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Podratçıya ödənəcək texnika xərci daxil edilməlidir");
            }
        }
        if (existing.getCustomerEquipmentPrice() == null || existing.getCustomerEquipmentPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Sifarişçiyə təklif ediləcək texnika qiyməti daxil edilməlidir");
        }
    }

    @Transactional
    public CoordinatorPlanResponse submitPlan(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        // Yeni flowda koordinator təklifi PM-ə qaytarır (mərkəzi gateway ilə)
        transitionService.transition(request, RequestStatus.COORDINATOR_PROPOSED, "Koordinator təklifi göndərildi", null);

        // Seçilmiş texnika(lar)ın statusunu İcarədə et
        planRepository.findByRequestId(requestId).ifPresent(plan -> {
            List<CoordinatorPlanItem> items = planItemRepository.findAllByPlanIdAndDeletedFalse(plan.getId());
            if (!items.isEmpty()) {
                for (CoordinatorPlanItem it : items) {
                    if (it.getEquipment() != null) {
                        equipmentService.changeStatus(it.getEquipment(), EquipmentStatus.RENTED,
                                "Koordinator təklifi göndərildi — texnika icarəyə alındı", equipmentService.currentUserOrNull());
                    }
                }
            } else {
                Equipment eq = plan.getSelectedEquipment() != null
                        ? plan.getSelectedEquipment()
                        : request.getSelectedEquipment();
                if (eq != null) {
                    equipmentService.changeStatus(eq, EquipmentStatus.RENTED,
                            "Koordinator təklifi göndərildi — texnika icarəyə alındı", equipmentService.currentUserOrNull());
                }
            }
        });

        notificationService.info("Təklif göndərildi", request.getRequestCode() + " üçün koordinator təklifi göndərildi", "COORDINATOR");

        return planRepository.findByRequestId(requestId)
                .map(CoordinatorPlanResponse::from)
                .orElseThrow();
    }

    // ─── Qəbul / Rədd ────────────────────────────────────────────────────────

    // Yeni flowda təklifin qəbulu Layihə Meneceri tərəfindən edilir.
    // Bu metodlar Dalğa 2-də ProjectManagerService-ə köçürüləcək — hələlik
    // backward-compat üçün saxlanılır, lakin işlədilmir.
    @Transactional
    public void acceptOffer(Long requestId) {
        throw new BusinessException("Yeni flowda təklifin qəbulu Layihə Meneceri tərəfindən edilir");
    }

    @Transactional
    public void rejectOffer(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        RequestStatus currentStatus = request.getStatus();
        // Yeni flowda hər mərhələdə imtina mümkündür
        if (currentStatus == RequestStatus.DELIVERED || currentStatus == RequestStatus.REJECTED) {
            throw new BusinessException("Bu statusda olan sorğu rədd edilə bilməz");
        }
        transitionService.transition(request, RequestStatus.REJECTED, "Koordinator tərəfindən rədd edildi", null);

        // Əgər texnika(lar) icarədə idi (Mərhələ B), onları Mövcud-a qaytar
        planRepository.findByRequestId(requestId).ifPresent(plan -> {
            List<CoordinatorPlanItem> items = planItemRepository.findAllByPlanIdAndDeletedFalse(plan.getId());
            if (!items.isEmpty()) {
                items.forEach(it -> releaseEquipment(it.getEquipment()));
            } else {
                Equipment eq = plan.getSelectedEquipment() != null
                        ? plan.getSelectedEquipment()
                        : request.getSelectedEquipment();
                releaseEquipment(eq);
            }
        });
    }

    /**
     * Geri qaytarma — koordinator öz təklifini geri alıb yenidən danışığa qayıdır.
     * COORDINATOR_PROPOSED → COORDINATOR_NEGOTIATING. Səbəb məcburi; texnika AVAILABLE-ə qaytarılır.
     */
    @Transactional
    public CoordinatorPlanResponse withdrawOffer(Long requestId, String reason) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.COORDINATOR_PROPOSED) {
            throw new BusinessException("Təklif yalnız COORDINATOR_PROPOSED statusunda geri alına bilər");
        }
        transitionService.transition(request, RequestStatus.COORDINATOR_NEGOTIATING, reason, null);
        releaseSelectedEquipment(request);
        return planRepository.findByRequestId(requestId).map(CoordinatorPlanResponse::from).orElse(null);
    }

    /**
     * Geri qaytarma — operatoru dəyişmək üçün icra mərhələsinə qayıt.
     * OPERATOR_ASSIGNED → EXECUTION_READY. Səbəb məcburi; plandakı operator təyini sıfırlanır.
     */
    @Transactional
    public CoordinatorPlanResponse resetOperator(Long requestId, String reason) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.OPERATOR_ASSIGNED) {
            throw new BusinessException("Operatoru dəyişmək yalnız OPERATOR_ASSIGNED statusunda mümkündür");
        }
        transitionService.transition(request, RequestStatus.EXECUTION_READY, reason, null);
        planRepository.findByRequestId(requestId).ifPresent(plan -> {
            plan.setOperator(null);
            planRepository.save(plan);
        });
        return planRepository.findByRequestId(requestId).map(CoordinatorPlanResponse::from).orElse(null);
    }

    /** Seçilmiş texnikanı (plandakı və ya sorğudakı) RENTED → AVAILABLE qaytarır. */
    private void releaseSelectedEquipment(TechRequest request) {
        planRepository.findByRequestId(request.getId()).ifPresentOrElse(plan -> {
            List<CoordinatorPlanItem> items = planItemRepository.findAllByPlanIdAndDeletedFalse(plan.getId());
            if (!items.isEmpty()) {
                items.forEach(it -> releaseEquipment(it.getEquipment()));
            } else {
                Equipment eq = plan.getSelectedEquipment() != null ? plan.getSelectedEquipment() : request.getSelectedEquipment();
                releaseEquipment(eq);
            }
        }, () -> releaseEquipment(request.getSelectedEquipment()));
    }

    private void releaseEquipment(Equipment eq) {
        if (eq != null && eq.getStatus() == EquipmentStatus.RENTED) {
            equipmentService.changeStatus(eq, EquipmentStatus.AVAILABLE,
                    "Təklif geri alındı — texnika azad edildi", equipmentService.currentUserOrNull());
        }
    }

    // ─── Texnika seçimi ───────────────────────────────────────────────────────

    @Transactional
    public CoordinatorPlanResponse selectEquipment(Long requestId, Long equipmentId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (!COORDINATOR_STATUSES.contains(request.getStatus())) {
            throw new BusinessException("Bu sorğu koordinator üçün uyğun deyil");
        }

        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseGet(() -> CoordinatorPlan.builder().request(request).build());

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", equipmentId));
        if (equipment.getStatus() == EquipmentStatus.RENTED) {
            throw new BusinessException("Bu texnika hazırda icarədədir və başqa layihəyə təyin edilə bilməz");
        }
        plan.setSelectedEquipment(equipment);

        return CoordinatorPlanResponse.from(planRepository.save(plan));
    }

    // ─── Mərhələ B: İcra (operator → yükləmə → təhvil-təslim) ───────────────

    @Transactional
    public CoordinatorPlanResponse assignOperator(Long requestId, Long operatorId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.EXECUTION_READY) {
            throw new BusinessException("Operator təyini yalnız EXECUTION_READY statusunda mümkündür");
        }
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Koordinator planı tapılmadı"));

        var operator = operatorRepository.findByIdActive(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator", operatorId));
        boolean busy = planRepository.isOperatorBusyInOtherProject(operatorId, requestId,
                List.of(ProjectStatus.PENDING, ProjectStatus.ACTIVE));
        if (busy) {
            throw new BusinessException("Bu operator artıq başqa aktiv layihəyə təyin edilib");
        }
        plan.setOperator(operator);
        planRepository.save(plan);

        changeRequestStatus(request, RequestStatus.OPERATOR_ASSIGNED, "Operator təyin edildi: " + operator.getFirstName());
        return CoordinatorPlanResponse.from(plan);
    }

    @Transactional
    public CoordinatorPlanResponse verifyEquipmentDocs(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.EXECUTION_READY
                && request.getStatus() != RequestStatus.OPERATOR_ASSIGNED) {
            throw new BusinessException("Sənəd yoxlaması yalnız icra mərhələsində mümkündür");
        }
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Koordinator planı tapılmadı"));

        // Bütün məcburi sənədlər işarələnməyincə yoxlama tamamlana bilməz
        java.util.Set<Long> required = requiredDocItemIds(plan);
        if (!plan.getCheckedDocumentItemIds().containsAll(required)) {
            throw new BusinessException("Bütün məcburi sənədlər işarələnməlidir");
        }

        plan.setEquipmentDocsVerified(true);
        plan.setEquipmentDocsCheckedAt(java.time.LocalDateTime.now());
        planRepository.save(plan);
        auditService.log("KOORDİNATOR", request.getId(), request.getRequestCode(),
                "SƏNƏD_YOXLANDI", "Texnika sənədləri yoxlanıldı");
        return CoordinatorPlanResponse.from(plan);
    }

    /** Yoxlama checklist-i üçün tək sənəd tipini işarələ / işarəni götür. */
    @Transactional
    public CoordinatorPlanResponse toggleDocCheck(Long requestId, Long configItemId, boolean checked) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.EXECUTION_READY
                && request.getStatus() != RequestStatus.OPERATOR_ASSIGNED) {
            throw new BusinessException("Sənəd yoxlaması yalnız icra mərhələsində mümkündür");
        }
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Koordinator planı tapılmadı"));
        if (checked) {
            plan.getCheckedDocumentItemIds().add(configItemId);
        } else {
            plan.getCheckedDocumentItemIds().remove(configItemId);
            // İşarə götürülürsə yoxlama "tamamlandı" statusu da geri alınır
            plan.setEquipmentDocsVerified(false);
            plan.setEquipmentDocsCheckedAt(null);
        }
        planRepository.save(plan);
        return CoordinatorPlanResponse.from(plan);
    }

    /** Texnikanın məcburi + LM-in əlavə etdiyi tələb olunan sənəd tiplərinin id-ləri. */
    private java.util.Set<Long> requiredDocItemIds(CoordinatorPlan plan) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        Equipment eq = plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : plan.getRequest().getSelectedEquipment();
        if (eq != null) {
            eq.getRequiredDocuments().forEach(ci -> ids.add(ci.getId()));
        }
        if (plan.getRequest() != null) {
            plan.getRequest().getExtraRequiredDocuments().forEach(ci -> ids.add(ci.getId()));
        }
        return ids;
    }

    @Transactional
    public CoordinatorPlanResponse dispatch(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.OPERATOR_ASSIGNED) {
            throw new BusinessException("Yükləmə yalnız OPERATOR_ASSIGNED statusunda mümkündür");
        }
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Koordinator planı tapılmadı"));
        if (!plan.isEquipmentDocsVerified()) {
            throw new BusinessException("Yükləmə üçün texnika sənədləri əvvəlcə yoxlanmalıdır");
        }
        plan.setDispatchedAt(java.time.LocalDateTime.now());
        planRepository.save(plan);

        // Texnikanı icarədə işarələ
        Equipment eq = plan.getSelectedEquipment();
        if (eq != null && eq.getStatus() != EquipmentStatus.RENTED) {
            equipmentService.changeStatus(eq, EquipmentStatus.RENTED,
                    "Texnika yükləndi və göndərildi", equipmentService.currentUserOrNull());
        }
        changeRequestStatus(request, RequestStatus.EQUIPMENT_DISPATCHED, "Texnika yükləndi və göndərildi");
        return CoordinatorPlanResponse.from(plan);
    }

    @Transactional
    public CoordinatorPlanResponse deliver(Long requestId, String notes) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.EQUIPMENT_DISPATCHED) {
            throw new BusinessException("Təhvil-təslim yalnız EQUIPMENT_DISPATCHED statusunda mümkündür");
        }
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Koordinator planı tapılmadı"));

        // Təhvil-təslim aktı yüklənməyincə tamamlanmasın
        boolean hasAct = !documentRepository
                .findAllByPlanIdAndDocumentTypeAndDeletedFalse(plan.getId(), "HANDOVER_ACT")
                .isEmpty();
        if (!hasAct) {
            throw new BusinessException("Təhvil-təslim aktı yüklənməyib — əvvəlcə aktı yükləyin");
        }

        plan.setDeliveredAt(java.time.LocalDateTime.now());
        if (notes != null && !notes.isBlank()) plan.setDeliveryNotes(notes);
        planRepository.save(plan);

        changeRequestStatus(request, RequestStatus.DELIVERED, "Təhvil-təslim tamamlandı");

        // QEYD: Təhvil-təslim layihəni AKTİVLƏŞDİRMİR — bu yalnız mühasibat OK + Əməliyyatların
        // təsdiqi ilə olur (DocumentCheckService). Burada yalnız təhvil qeydi + texnika statusu (gateway).

        return CoordinatorPlanResponse.from(plan);
    }

    // ═══════════════ İcra — hər texnika xətti ayrı (çoxlu model) ═══════════════

    private CoordinatorPlanItem findItemOrThrow(Long requestId, Long itemId) {
        CoordinatorPlanItem item = planItemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika xətti", itemId));
        if (item.getPlan() == null || item.getPlan().getRequest() == null
                || !item.getPlan().getRequest().getId().equals(requestId)) {
            throw new BusinessException("Bu xətt bu sorğuya aid deyil");
        }
        return item;
    }

    private void requireExecution(TechRequest request) {
        if (request.getStatus() != RequestStatus.EXECUTION_READY) {
            throw new BusinessException("Bu əməliyyat yalnız icra mərhələsində (EXECUTION_READY) mümkündür");
        }
    }

    private CoordinatorPlanResponse planResponse(Long requestId) {
        return planRepository.findByRequestId(requestId).map(CoordinatorPlanResponse::from).orElseThrow();
    }

    /** Bütün xətlər təhvil verildikdə sorğunu aqreqat olaraq DELIVERED-ə keçir. */
    private void maybeCompleteDelivery(TechRequest request, CoordinatorPlan plan) {
        List<CoordinatorPlanItem> items = planItemRepository.findAllByPlanIdAndDeletedFalse(plan.getId());
        if (items.isEmpty()) return;
        boolean allDelivered = items.stream().allMatch(it -> it.getDeliveredAt() != null);
        if (allDelivered && request.getStatus() == RequestStatus.EXECUTION_READY) {
            changeRequestStatus(request, RequestStatus.DELIVERED, "Bütün texnikalar təhvil verildi");
        }
    }

    /** Bir xəttin məcburi sənəd tiplərinin id-ləri (texnika + LM-in əlavələri). */
    private java.util.Set<Long> requiredDocItemIdsForItem(CoordinatorPlanItem item) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (item.getEquipment() != null) {
            item.getEquipment().getRequiredDocuments().forEach(ci -> ids.add(ci.getId()));
        }
        TechRequest req = item.getPlan() != null ? item.getPlan().getRequest() : null;
        if (req != null) {
            req.getExtraRequiredDocuments().forEach(ci -> ids.add(ci.getId()));
        }
        return ids;
    }

    @Transactional
    public CoordinatorPlanResponse assignOperatorItem(Long requestId, Long itemId, Long operatorId) {
        TechRequest request = findRequestOrThrow(requestId);
        requireExecution(request);
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        var operator = operatorRepository.findByIdActive(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator", operatorId));
        item.setOperator(operator);
        planItemRepository.save(item);
        return planResponse(requestId);
    }

    @Transactional
    public CoordinatorPlanResponse resetOperatorItem(Long requestId, Long itemId, String reason) {
        TechRequest request = findRequestOrThrow(requestId);
        requireExecution(request);
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        if (item.getDispatchedAt() != null) {
            throw new BusinessException("Texnika göndərildikdən sonra operator dəyişdirilə bilməz");
        }
        item.setOperator(null);
        planItemRepository.save(item);
        return planResponse(requestId);
    }

    @Transactional
    public CoordinatorPlanResponse toggleDocCheckItem(Long requestId, Long itemId, Long configItemId, boolean checked) {
        TechRequest request = findRequestOrThrow(requestId);
        requireExecution(request);
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        if (checked) {
            item.getCheckedDocumentItemIds().add(configItemId);
        } else {
            item.getCheckedDocumentItemIds().remove(configItemId);
            item.setEquipmentDocsVerified(false);
            item.setEquipmentDocsCheckedAt(null);
        }
        planItemRepository.save(item);
        return planResponse(requestId);
    }

    @Transactional
    public CoordinatorPlanResponse verifyDocsItem(Long requestId, Long itemId) {
        TechRequest request = findRequestOrThrow(requestId);
        requireExecution(request);
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        java.util.Set<Long> required = requiredDocItemIdsForItem(item);
        if (!item.getCheckedDocumentItemIds().containsAll(required)) {
            throw new BusinessException("Bütün məcburi sənədlər işarələnməlidir");
        }
        item.setEquipmentDocsVerified(true);
        item.setEquipmentDocsCheckedAt(java.time.LocalDateTime.now());
        planItemRepository.save(item);
        return planResponse(requestId);
    }

    @Transactional
    public CoordinatorPlanResponse dispatchItem(Long requestId, Long itemId) {
        TechRequest request = findRequestOrThrow(requestId);
        requireExecution(request);
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        if (item.getOperator() == null) {
            throw new BusinessException("Göndərmədən əvvəl operator təyin edilməlidir");
        }
        if (!item.isEquipmentDocsVerified()) {
            throw new BusinessException("Göndərmədən əvvəl texnika sənədləri yoxlanmalıdır");
        }
        item.setDispatchedAt(java.time.LocalDateTime.now());
        planItemRepository.save(item);
        return planResponse(requestId);
    }

    @Transactional
    public CoordinatorPlanResponse deliverItem(Long requestId, Long itemId, String notes) {
        TechRequest request = findRequestOrThrow(requestId);
        requireExecution(request);
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        if (item.getDispatchedAt() == null) {
            throw new BusinessException("Təhvil-təslimdən əvvəl texnika göndərilməlidir");
        }
        boolean hasAct = !documentRepository
                .findAllByPlanItemIdAndDocumentTypeAndDeletedFalse(item.getId(), "HANDOVER_ACT")
                .isEmpty();
        if (!hasAct) {
            throw new BusinessException("Təhvil-təslim aktı yüklənməyib — əvvəlcə aktı yükləyin");
        }
        item.setDeliveredAt(java.time.LocalDateTime.now());
        if (notes != null && !notes.isBlank()) item.setDeliveryNotes(notes);
        planItemRepository.save(item);
        maybeCompleteDelivery(request, item.getPlan());
        return planResponse(requestId);
    }

    /** Xəttə sənəd (təhvil-təslim aktı və s.) yüklə. */
    @Transactional
    public CoordinatorPlanResponse uploadItemDocument(Long requestId, Long itemId, MultipartFile file,
                                                      String documentType, Long userId) {
        CoordinatorPlanItem item = findItemOrThrow(requestId, itemId);
        CoordinatorPlan plan = item.getPlan();
        // Eyni tipli köhnə sənədi əvəz et (akt tək olur)
        if (documentType != null && !documentType.equals("OTHER")) {
            for (CoordinatorDocument old : documentRepository
                    .findAllByPlanItemIdAndDocumentTypeAndDeletedFalse(item.getId(), documentType)) {
                fileStorageService.delete(old.getFilePath());
                old.softDelete();
                documentRepository.save(old);
            }
        }
        String path = fileStorageService.store(file, "coordinator-documents");
        String original = file.getOriginalFilename();
        String fileType = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf(".") + 1).toUpperCase() : "FILE";
        CoordinatorDocument doc = CoordinatorDocument.builder()
                .plan(plan)
                .planItem(item)
                .filePath(path)
                .documentName(original)
                .fileType(fileType)
                .documentType(documentType)
                .uploadedBy(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .build();
        documentRepository.save(doc);
        return planResponse(requestId);
    }

    /** Bütün koordinator status keçidləri mərkəzi gateway-dən keçir (validasiya + log + audit). */
    private void changeRequestStatus(TechRequest r, RequestStatus newStatus, String reason) {
        transitionService.transition(r, newStatus, reason, null);
    }

    // ─── Sənədlər ─────────────────────────────────────────────────────────────

    @Transactional
    public CoordinatorPlanResponse.DocumentDto uploadDocument(Long requestId, MultipartFile file,
                                                               String documentName, String documentType,
                                                               Long userId) {
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Əvvəlcə koordinator planını yadda saxlayın"));

        // Məcburi tək sənəd yenidən yüklənirsə — əvvəlkini sil (koordinator + qaraj).
        // OTHER və DISPATCH_PHOTO çoxlu fayla icazə verir — köhnələr saxlanılır.
        boolean replacePrevious = documentType != null
                && !documentType.equals("OTHER")
                && !documentType.equals("DISPATCH_PHOTO");
        if (replacePrevious) {
            List<CoordinatorDocument> oldDocs = documentRepository
                    .findAllByPlanIdAndDocumentTypeAndDeletedFalse(plan.getId(), documentType);
            for (CoordinatorDocument old : oldDocs) {
                fileStorageService.delete(old.getFilePath());
                old.softDelete();
                documentRepository.save(old);
            }
            if (plan.getSelectedEquipment() != null) {
                List<EquipmentDocument> oldEqDocs = equipmentDocumentRepository
                        .findAllByEquipmentIdAndDocumentType(plan.getSelectedEquipment().getId(), documentType);
                for (EquipmentDocument old : oldEqDocs) {
                    fileStorageService.delete(old.getFilePath());
                    equipmentDocumentRepository.delete(old);
                }
            }
        }

        String path = fileStorageService.store(file, "coordinator-documents");
        String original = file.getOriginalFilename();
        String fileType = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf(".") + 1).toUpperCase()
                : "FILE";

        CoordinatorDocument doc = CoordinatorDocument.builder()
                .plan(plan)
                .filePath(path)
                .documentName(documentName != null && !documentName.isBlank() ? documentName : original)
                .fileType(fileType)
                .documentType(documentType)
                .uploadedBy(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .build();

        CoordinatorDocument saved = documentRepository.save(doc);

        // Koordinator sənədini eyni zamanda texnikaya da əlavə et (qarajda görünsün)
        if (plan.getSelectedEquipment() != null) {
            EquipmentDocument eqDoc = EquipmentDocument.builder()
                    .equipment(plan.getSelectedEquipment())
                    .documentName(saved.getDocumentName())
                    .filePath(path)
                    .fileType(fileType)
                    .documentType(documentType)
                    .uploadedBy(saved.getUploadedBy())
                    .build();
            equipmentDocumentRepository.save(eqDoc);
        }

        return CoordinatorPlanResponse.DocumentDto.builder()
                .id(saved.getId())
                .documentName(saved.getDocumentName())
                .fileType(saved.getFileType())
                .documentType(saved.getDocumentType())
                .uploadedByName(saved.getUploadedBy() != null ? saved.getUploadedBy().getFullName() : null)
                .uploadedAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteDocument(Long requestId, Long documentId) {
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", requestId));
        CoordinatorDocument doc = documentRepository.findByIdAndPlanIdAndDeletedFalse(documentId, plan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        fileStorageService.delete(doc.getFilePath());
        doc.softDelete();
        documentRepository.save(doc);
    }

    public Path resolveDocument(Long requestId, Long documentId) {
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", requestId));
        CoordinatorDocument doc = documentRepository.findByIdAndPlanIdAndDeletedFalse(documentId, plan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        return fileStorageService.resolve(doc.getFilePath());
    }

    /**
     * Texnikanın qarajda yüklənmiş sənədini koordinator yoxlaması üçün aç.
     * Təhlükəsizlik: sənəd bu sorğunun planındakı texnikalardan birinə aid olmalıdır.
     */
    public Path resolveEquipmentDocument(Long requestId, Long documentId) {
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", requestId));
        EquipmentDocument doc = equipmentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika sənədi", documentId));
        boolean belongs = doc.getEquipment() != null && planItemRepository.findAllByPlanIdAndDeletedFalse(plan.getId())
                .stream()
                .anyMatch(it -> it.getEquipment() != null
                        && it.getEquipment().getId().equals(doc.getEquipment().getId()));
        if (!belongs) {
            throw new ResourceNotFoundException("Texnika sənədi", documentId);
        }
        return fileStorageService.resolve(doc.getFilePath());
    }

    /**
     * Müqavilə sənədini (müştəri/sahib) koordinator oxu-rejimi üçün aç.
     * Təhlükəsizlik: sənəd bu sorğuya aid olmalıdır.
     */
    public Path resolveRequestDocument(Long requestId, Long documentId) {
        var doc = requestDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        if (doc.isDeleted() || doc.getRequest() == null || !doc.getRequest().getId().equals(requestId)) {
            throw new ResourceNotFoundException("Sənəd", documentId);
        }
        return fileStorageService.resolve(doc.getFilePath());
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private TechRequest findRequestOrThrow(Long requestId) {
        return requestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", requestId));
    }
}
