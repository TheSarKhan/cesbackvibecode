package com.ces.erp.projectmanager.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.coordinator.dto.CoordinatorPlanResponse;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.request.service.RequestTransitionService;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.projectmanager.dto.CustomerAgreementRequest;
import com.ces.erp.projectmanager.dto.CustomerContactRequest;
import com.ces.erp.projectmanager.dto.PmRequestResponse;
import com.ces.erp.projectmanager.dto.ShortlistItemDto;
import com.ces.erp.projectmanager.dto.ShortlistSaveRequest;
import com.ces.erp.projectmanager.entity.PartyType;
import com.ces.erp.projectmanager.entity.RequestShortlist;
import com.ces.erp.projectmanager.entity.ShortlistItem;
import com.ces.erp.projectmanager.repository.RequestShortlistRepository;
import com.ces.erp.projectmanager.repository.ShortlistItemRepository;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.request.entity.RequestStatusLog;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.RequestDocumentRepository;
import com.ces.erp.request.repository.RequestStatusLogRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectManagerService implements ApprovalHandler {

    private final TechRequestRepository requestRepository;
    private final RequestStatusLogRepository statusLogRepository;
    private final RequestShortlistRepository shortlistRepository;
    private final ShortlistItemRepository shortlistItemRepository;
    private final CoordinatorPlanRepository coordinatorPlanRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final EquipmentRepository equipmentRepository;
    private final com.ces.erp.garage.service.EquipmentService equipmentService;
    private final ProjectRepository projectRepository;
    private final RequestDocumentRepository requestDocumentRepository;
    private final AuditService auditService;
    private final RequestTransitionService transitionService;

    // PM-in görəcəyi statuslar
    private static final List<RequestStatus> PM_STATUSES = List.of(
            RequestStatus.PENDING,
            RequestStatus.PM_REVIEW,
            RequestStatus.PM_SHORTLIST_READY,
            RequestStatus.COORDINATOR_PROPOSED,
            RequestStatus.PM_PRICE_NEGOTIATION,
            RequestStatus.PM_APPROVED,
            RequestStatus.REJECTED
    );

    @Override public String getEntityType() { return "PM_APPROVE"; }
    @Override public String getModuleCode()  { return "PROJECT_MANAGER"; }
    @Override public String getLabel(Long id) {
        return requestRepository.findByIdAndDeletedFalse(id)
                .map(TechRequest::getRequestCode).orElse("Sorğu #" + id);
    }
    @Override public Object getSnapshot(Long id) { return getRequest(id); }

    @Override
    public void applyEdit(Long id, String json) {
        ApprovalContext.setApplying(true);
        try { approveInternal(id); } finally { ApprovalContext.clear(); }
    }

    @Override public void applyDelete(Long id) { /* istifadə edilmir */ }

    // ─── Siyahı və detal ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PmRequestResponse> getRequests() {
        return requestRepository.findAllByStatusInAndDeletedFalse(PM_STATUSES).stream()
                .map(PmRequestResponse::fromList)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<PmRequestResponse> getRequestsPaged(int page, int size, String search, String status,
                                                              String sortBy, String sortDir) {
        String q = (search != null && !search.isBlank()) ? search : null;
        RequestStatus s = null;
        if (status != null && !status.isBlank()) {
            try { s = RequestStatus.valueOf(status); } catch (IllegalArgumentException ignored) {}
        }
        if (s != null && !PM_STATUSES.contains(s)) s = null;

        String field = sortBy != null ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(field).ascending() : Sort.by(field).descending();
        var pageable = PageRequest.of(page, size, sort);
        var result = requestRepository.findAllProjectManagerFiltered(q, s, pageable);
        return PagedResponse.from(result, PmRequestResponse::fromList);
    }

    @Transactional(readOnly = true)
    public Map<RequestStatus, Long> getStats() {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : PM_STATUSES) result.put(s, 0L);
        requestRepository.countGroupedByStatusIn(PM_STATUSES)
                .forEach(row -> result.put(row.getStatus(), row.getCnt()));
        return result;
    }

    @Transactional(readOnly = true)
    public PmRequestResponse getRequest(Long requestId) {
        TechRequest request = findOrThrow(requestId);

        PmRequestResponse resp = PmRequestResponse.fromList(request);

        // Texniki parametrlər
        if (request.getParams() != null) {
            resp.setParams(request.getParams().stream()
                    .map(p -> PmRequestResponse.ParamDto.builder()
                            .paramKey(p.getParamKey())
                            .paramValue(p.getParamValue())
                            .build())
                    .toList());
        }

        // Shortlist
        shortlistRepository.findByRequestIdAndDeletedFalse(requestId).ifPresent(sl -> {
            resp.setShortlistId(sl.getId());
            resp.setShortlistNotes(sl.getNotes());
            resp.setShortlistItems(shortlistItemRepository.findAllByShortlistIdAndDeletedFalse(sl.getId()).stream()
                    .map(ShortlistItemDto::from)
                    .toList());
        });

        // Koordinator təklifi
        coordinatorPlanRepository.findByRequestId(requestId).ifPresent(plan -> {
            resp.setCoordinatorOffer(CoordinatorPlanResponse.from(plan));
        });

        // PM-in yüklədiyi sənədlər (müqavilə + qiymət protokolu)
        var docs = requestDocumentRepository.findAllByRequestIdAndDeletedFalse(requestId);
        resp.setContractUploaded(docs.stream().anyMatch(d -> d.getDocType() == RequestDocumentType.CONTRACT));
        resp.setPriceProtocolUploaded(docs.stream().anyMatch(d -> d.getDocType() == RequestDocumentType.PRICE_PROTOCOL));
        resp.setDocuments(docs.stream().map(d -> PmRequestResponse.DocumentDto.builder()
                .id(d.getId())
                .docType(d.getDocType().name())
                .fileName(d.getFileName())
                .uploadedByName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : null)
                .uploadedAt(d.getCreatedAt())
                .build()).toList());

        return resp;
    }

    // ─── Status keçidləri ────────────────────────────────────────────────────

    @Transactional
    public PmRequestResponse accept(Long requestId) {
        TechRequest r = findOrThrow(requestId);
        requireStatus(r, RequestStatus.PENDING);
        changeStatus(r, RequestStatus.PM_REVIEW, "PM qəbul etdi");
        return getRequest(requestId);
    }

    @Transactional
    public PmRequestResponse sendToCoordinator(Long requestId) {
        TechRequest r = findOrThrow(requestId);
        requireStatusIn(r, Set.of(RequestStatus.PM_REVIEW, RequestStatus.PM_SHORTLIST_READY));

        if (!shortlistRepository.existsByRequestIdAndDeletedFalse(requestId)) {
            throw new BusinessException("Koordinatora göndərmək üçün ən az bir shortlist sətri olmalıdır");
        }
        RequestShortlist sl = shortlistRepository.findByRequestIdAndDeletedFalse(requestId).orElseThrow();
        if (shortlistItemRepository.findAllByShortlistIdAndDeletedFalse(sl.getId()).isEmpty()) {
            throw new BusinessException("Shortlist boşdur — sətir əlavə edin");
        }

        // SHORTLIST_READY mərhələsinə keçirib oradan COORDINATOR_NEGOTIATING-ə keçir
        if (r.getStatus() == RequestStatus.PM_REVIEW) {
            changeStatus(r, RequestStatus.PM_SHORTLIST_READY, "Shortlist hazırdır");
        }
        changeStatus(r, RequestStatus.COORDINATOR_NEGOTIATING, "Koordinatora yönləndirildi");
        return getRequest(requestId);
    }

    @Transactional
    public PmRequestResponse saveCustomerAgreement(Long requestId, CustomerAgreementRequest req) {
        TechRequest r = findOrThrow(requestId);
        requireStatusIn(r, Set.of(RequestStatus.COORDINATOR_PROPOSED, RequestStatus.PM_PRICE_NEGOTIATION));

        if (req.getAgreedEquipmentPrice() != null) {
            r.setAgreedEquipmentPrice(req.getAgreedEquipmentPrice());
        }
        if (req.getAgreedTransportPrice() != null) {
            r.setAgreedTransportPrice(req.getAgreedTransportPrice());
        }
        // Cəmi: ya client göndərdiyini götür, ya texnika+daşınma cəmini
        java.math.BigDecimal eq = r.getAgreedEquipmentPrice() != null ? r.getAgreedEquipmentPrice() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal tr = r.getAgreedTransportPrice() != null ? r.getAgreedTransportPrice() : java.math.BigDecimal.ZERO;
        if (req.getAgreedTotalPrice() != null) {
            r.setAgreedTotalPrice(req.getAgreedTotalPrice());
        } else if (req.getAgreedEquipmentPrice() != null || req.getAgreedTransportPrice() != null) {
            r.setAgreedTotalPrice(eq.add(tr));
        }
        if (req.getAgreementNote() != null && !req.getAgreementNote().isBlank()) {
            String existing = r.getNotes() != null ? r.getNotes() + "\n" : "";
            r.setNotes(existing + "[Razılaşma] " + req.getAgreementNote());
        }
        // COORDINATOR_PROPOSED → PM_PRICE_NEGOTIATION (ilk dəfə açıldıqda)
        if (r.getStatus() == RequestStatus.COORDINATOR_PROPOSED) {
            changeStatus(r, RequestStatus.PM_PRICE_NEGOTIATION, "Sifarişçi ilə danışıq başladı");
        } else {
            requestRepository.save(r);
        }
        return getRequest(requestId);
    }

    /**
     * LM addımı 1.3 — Sifarişçi ofisindəki əlaqə şəxsini qeyd et.
     * PM sorğunu PM_REVIEW və ya PM_SHORTLIST_READY statusunda iken dəyişə bilər.
     */
    @Transactional
    public PmRequestResponse saveCustomerContact(Long requestId, CustomerContactRequest req) {
        TechRequest r = findOrThrow(requestId);
        requireStatusIn(r, Set.of(RequestStatus.PM_REVIEW, RequestStatus.PM_SHORTLIST_READY));
        r.setCustomerOfficeContact(req.getCustomerOfficeContact());
        r.setCustomerOfficePhone(req.getCustomerOfficePhone());
        requestRepository.save(r);
        return getRequest(requestId);
    }

    @Transactional
    @RequiresApproval(module = "PROJECT_MANAGER", entityType = "PM_APPROVE")
    public PmRequestResponse approve(Long requestId) {
        approveInternal(requestId);
        return getRequest(requestId);
    }

    private void approveInternal(Long requestId) {
        TechRequest r = findOrThrow(requestId);
        requireStatus(r, RequestStatus.PM_PRICE_NEGOTIATION);

        changeStatus(r, RequestStatus.PM_APPROVED, "PM təsdiqlədi");

        // Layihə artıq yaradılmayıbsa — PENDING statuslu yarat
        if (!projectRepository.existsByRequestIdAndDeletedFalse(requestId)) {
            int nextNum = projectRepository.findMaxProjectCodeNumber() + 1;
            Project project = Project.builder()
                    .projectCode("PRJ-" + String.format("%04d", nextNum))
                    .request(r)
                    .status(ProjectStatus.PENDING)
                    .build();
            projectRepository.save(project);
        }

        // Növbəti addım — mühasibatlığa keçid avtomatik (sənəd yoxlaması)
        changeStatus(r, RequestStatus.ACCOUNTING_DOCS_CHECK, "Mühasibatlığa göndərildi");
    }

    @Transactional
    public PmRequestResponse reject(Long requestId, String reason) {
        TechRequest r = findOrThrow(requestId);
        if (r.getStatus() == RequestStatus.DELIVERED || r.getStatus() == RequestStatus.REJECTED) {
            throw new BusinessException("Bu statusda olan sorğu rədd edilə bilməz");
        }
        changeStatus(r, RequestStatus.REJECTED, reason != null ? reason : "PM tərəfindən rədd edildi");

        // Yaradılmış pending Project varsa onu da soft-delete et
        projectRepository.findByRequestIdAndDeletedFalse(requestId).ifPresent(p -> {
            if (p.getStatus() == ProjectStatus.PENDING) {
                p.softDelete();
                projectRepository.save(p);
            }
        });
        return getRequest(requestId);
    }

    /**
     * Geri qaytarma — LM müştəri ilə qiyməti tuta bilmədi, koordinatordan yeni təklif istəyir.
     * PM_PRICE_NEGOTIATION → COORDINATOR_NEGOTIATING. Səbəb məcburidir; seçilmiş texnika AVAILABLE-ə qaytarılır.
     */
    @Transactional
    public PmRequestResponse sendBackToCoordinator(Long requestId, String reason) {
        TechRequest r = findOrThrow(requestId);
        requireStatus(r, RequestStatus.PM_PRICE_NEGOTIATION);
        transitionService.transition(r, RequestStatus.COORDINATOR_NEGOTIATING, reason, null);
        releaseSelectedEquipment(r);
        return getRequest(requestId);
    }

    // ─── Shortlist ───────────────────────────────────────────────────────────

    @Transactional
    public PmRequestResponse saveShortlist(Long requestId, ShortlistSaveRequest req) {
        TechRequest r = findOrThrow(requestId);
        requireStatusIn(r, Set.of(RequestStatus.PM_REVIEW, RequestStatus.PM_SHORTLIST_READY));

        RequestShortlist sl = shortlistRepository.findByRequestIdAndDeletedFalse(requestId)
                .orElseGet(() -> RequestShortlist.builder().request(r).build());
        sl.setNotes(req.getNotes());
        sl = shortlistRepository.save(sl);

        // Mövcud sətirləri al
        final Long shortlistId = sl.getId();
        List<ShortlistItem> existing = shortlistItemRepository.findAllByShortlistIdAndDeletedFalse(shortlistId);
        Set<Long> incomingIds = req.getItems() == null ? Set.of() :
                req.getItems().stream().map(ShortlistSaveRequest.Item::getId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());

        // Mövcud sətirlərdən gəlməyənləri soft-delete et
        for (ShortlistItem old : existing) {
            if (!incomingIds.contains(old.getId())) {
                old.softDelete();
                shortlistItemRepository.save(old);
            }
        }

        // Yeni və ya update sətirləri saxla
        if (req.getItems() != null) {
            for (ShortlistSaveRequest.Item itemReq : req.getItems()) {
                if (itemReq.getPartyType() == null) {
                    throw new BusinessException("Hər sətrin tipi (Şirkət/Podratçı/Investor) seçilməlidir");
                }
                if (itemReq.getEquipmentId() == null) {
                    throw new BusinessException("Hər sətrdə texnika seçilməlidir");
                }
                boolean hasContractor = itemReq.getContractorId() != null;
                boolean hasInvestor = itemReq.getInvestorId() != null;
                if (itemReq.getPartyType() == PartyType.COMPANY && (hasContractor || hasInvestor)) {
                    throw new BusinessException("Şirkət sətrində podratçı/investor seçilə bilməz");
                }
                if (itemReq.getPartyType() == PartyType.CONTRACTOR && (!hasContractor || hasInvestor)) {
                    throw new BusinessException("Podratçı sətrində yalnız Podratçı seçilməlidir");
                }
                if (itemReq.getPartyType() == PartyType.INVESTOR && (!hasInvestor || hasContractor)) {
                    throw new BusinessException("Investor sətrində yalnız Investor seçilməlidir");
                }

                ShortlistItem item = itemReq.getId() != null
                        ? shortlistItemRepository.findById(itemReq.getId()).orElse(null)
                        : null;
                if (item == null) {
                    item = ShortlistItem.builder().shortlist(sl).build();
                }

                item.setPartyType(itemReq.getPartyType());
                item.setContractor(hasContractor
                        ? contractorRepository.findById(itemReq.getContractorId())
                            .orElseThrow(() -> new ResourceNotFoundException("Podratçı", itemReq.getContractorId()))
                        : null);
                item.setInvestor(hasInvestor
                        ? investorRepository.findById(itemReq.getInvestorId())
                            .orElseThrow(() -> new ResourceNotFoundException("Investor", itemReq.getInvestorId()))
                        : null);
                item.setEquipment(itemReq.getEquipmentId() != null
                        ? equipmentRepository.findById(itemReq.getEquipmentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Texnika", itemReq.getEquipmentId()))
                        : null);
                item.setNegotiatedPrice(itemReq.getNegotiatedPrice());
                item.setRank(itemReq.getRank());
                item.setNotes(itemReq.getNotes());
                shortlistItemRepository.save(item);
            }
        }

        // Shortlist boş deyilsə statusu PM_SHORTLIST_READY-ə qaldır
        boolean hasItems = !shortlistItemRepository.findAllByShortlistIdAndDeletedFalse(shortlistId).isEmpty();
        if (hasItems && r.getStatus() == RequestStatus.PM_REVIEW) {
            changeStatus(r, RequestStatus.PM_SHORTLIST_READY, "Shortlist hazırdır");
        }

        return getRequest(requestId);
    }

    @Transactional
    public void deleteShortlistItem(Long requestId, Long itemId) {
        TechRequest r = findOrThrow(requestId);
        requireStatusIn(r, Set.of(RequestStatus.PM_REVIEW, RequestStatus.PM_SHORTLIST_READY));
        RequestShortlist sl = shortlistRepository.findByRequestIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Shortlist", requestId));
        ShortlistItem item = shortlistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shortlist sətri", itemId));
        if (!item.getShortlist().getId().equals(sl.getId())) {
            throw new BusinessException("Bu sətr bu sorğuya aid deyil");
        }
        item.softDelete();
        shortlistItemRepository.save(item);
    }

    // ─── Köməkçi ─────────────────────────────────────────────────────────────

    private TechRequest findOrThrow(Long requestId) {
        return requestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", requestId));
    }

    private void requireStatus(TechRequest r, RequestStatus expected) {
        if (r.getStatus() != expected) {
            throw new BusinessException("Sorğu " + expected.name() + " statusunda olmalıdır (hazırda: " + r.getStatus().name() + ")");
        }
    }

    private void requireStatusIn(TechRequest r, Set<RequestStatus> allowed) {
        if (!allowed.contains(r.getStatus())) {
            throw new BusinessException("Bu əməliyyat üçün uyğun status deyil (hazırda: " + r.getStatus().name() + ")");
        }
    }

    /** Bütün PM status keçidləri mərkəzi gateway-dən keçir (validasiya + log + audit). */
    private void changeStatus(TechRequest r, RequestStatus newStatus, String reason) {
        transitionService.transition(r, newStatus, reason, null);
    }

    /** Seçilmiş texnikanı (koordinator planındakı və ya sorğudakı) AVAILABLE-ə qaytarır. */
    private void releaseSelectedEquipment(TechRequest r) {
        coordinatorPlanRepository.findByRequestId(r.getId()).ifPresentOrElse(plan -> {
            Equipment eq = plan.getSelectedEquipment() != null ? plan.getSelectedEquipment() : r.getSelectedEquipment();
            releaseEquipment(eq);
        }, () -> releaseEquipment(r.getSelectedEquipment()));
    }

    private void releaseEquipment(Equipment eq) {
        if (eq != null && eq.getStatus() == EquipmentStatus.RENTED) {
            equipmentService.changeStatus(eq, EquipmentStatus.AVAILABLE,
                    "Layihə meneceri texnikanı azad etdi", equipmentService.currentUserOrNull());
        }
    }
}
