package com.ces.erp.coordinator.service;

import com.ces.erp.approval.annotation.RequiresApproval;
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
    private final CoordinatorDocumentRepository documentRepository;
    private final EquipmentRepository equipmentRepository;
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
        return resp;
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

        CoordinatorPlanResponse resp = CoordinatorPlanResponse.from(planRepository.save(plan));
        resp.setShortlistItems(loadShortlistRows(requestId));
        return resp;
    }

    public void validateBeforeSubmit(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.COORDINATOR_NEGOTIATING) {
            throw new BusinessException("Plan yalnız COORDINATOR_NEGOTIATING statusunda göndərilə bilər");
        }
        CoordinatorPlan existing = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Əvvəlcə koordinator planını doldurun"));
        if (existing.getWinnerItem() == null) {
            throw new BusinessException("Shortlist-dən qalib sətir seçilməlidir");
        }
        if (existing.getWinnerItem().getEquipment() == null) {
            throw new BusinessException("Qalib sətrdə texnika qeyd edilməlidir");
        }
        // Şirkət texnikasında ödəniş yoxdur — yalnız xarici (podratçı/investor) üçün xərc tələb olunur
        boolean winnerIsCompany = existing.getWinnerItem() != null
                && existing.getWinnerItem().getPartyType() == com.ces.erp.projectmanager.entity.PartyType.COMPANY;
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
    @RequiresApproval(module = "COORDINATOR", entityType = "COORDINATOR_SUBMIT")
    public CoordinatorPlanResponse submitPlan(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        // Yeni flowda koordinator təklifi PM-ə qaytarır
        request.setStatus(RequestStatus.COORDINATOR_PROPOSED);
        requestRepository.save(request);

        // Seçilmiş texnikanın statusunu İcarədə et
        planRepository.findByRequestId(requestId).ifPresent(plan -> {
            Equipment eq = plan.getSelectedEquipment() != null
                    ? plan.getSelectedEquipment()
                    : request.getSelectedEquipment();
            if (eq != null) {
                eq.setStatus(EquipmentStatus.RENTED);
                equipmentRepository.save(eq);
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
        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);

        // Əgər texnika icarədə idi (Mərhələ B), onu Mövcud-a qaytar
        planRepository.findByRequestId(requestId).ifPresent(plan -> {
            Equipment eq = plan.getSelectedEquipment() != null
                    ? plan.getSelectedEquipment()
                    : request.getSelectedEquipment();
            if (eq != null && eq.getStatus() == EquipmentStatus.RENTED) {
                eq.setStatus(EquipmentStatus.AVAILABLE);
                equipmentRepository.save(eq);
            }
        });
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
        plan.setEquipmentDocsVerified(true);
        plan.setEquipmentDocsCheckedAt(java.time.LocalDateTime.now());
        planRepository.save(plan);
        auditService.log("KOORDİNATOR", request.getId(), request.getRequestCode(),
                "SƏNƏD_YOXLANDI", "Texnika sənədləri yoxlanıldı");
        return CoordinatorPlanResponse.from(plan);
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
            eq.setStatus(EquipmentStatus.RENTED);
            equipmentRepository.save(eq);
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
        plan.setDeliveredAt(java.time.LocalDateTime.now());
        if (notes != null && !notes.isBlank()) plan.setDeliveryNotes(notes);
        planRepository.save(plan);

        changeRequestStatus(request, RequestStatus.DELIVERED, "Təhvil-təslim tamamlandı");

        // Project-i ACTIVE et (yaradılıbsa)
        projectRepository.findByRequestIdAndDeletedFalse(requestId).ifPresent(project -> {
            if (project.getStatus() == ProjectStatus.PENDING) {
                project.setStatus(ProjectStatus.ACTIVE);
                if (plan.getStartDate() != null) project.setStartDate(plan.getStartDate());
                if (plan.getEndDate() != null) project.setEndDate(plan.getEndDate());
                projectRepository.save(project);
            }
        });

        return CoordinatorPlanResponse.from(plan);
    }

    private void changeRequestStatus(TechRequest r, RequestStatus newStatus, String reason) {
        RequestStatus old = r.getStatus();
        r.setStatus(newStatus);
        requestRepository.save(r);

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        statusLogRepository.save(com.ces.erp.request.entity.RequestStatusLog.builder()
                .requestId(r.getId())
                .oldStatus(old)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(username)
                .build());
        auditService.log("SORĞU", r.getId(),
                r.getRequestCode() != null ? r.getRequestCode() : "REQ-" + r.getId(),
                "STATUS_DƏYİŞDİ",
                old.name() + " → " + newStatus.name() + (reason != null ? " | " + reason : ""));
    }

    // ─── Sənədlər ─────────────────────────────────────────────────────────────

    @Transactional
    public CoordinatorPlanResponse.DocumentDto uploadDocument(Long requestId, MultipartFile file,
                                                               String documentName, String documentType,
                                                               Long userId) {
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Əvvəlcə koordinator planını yadda saxlayın"));

        // Məcburi sənəd yenidən yüklənirsə — əvvəlkini sil (koordinator + qaraj)
        if (documentType != null && !documentType.equals("OTHER")) {
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

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private TechRequest findRequestOrThrow(Long requestId) {
        return requestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", requestId));
    }
}
