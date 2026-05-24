package com.ces.erp.request.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.InvalidStatusTransitionException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.websocket.NotificationService;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.request.dto.StatusLogResponse;
import com.ces.erp.request.dto.TechRequestRequest;
import com.ces.erp.request.dto.TechRequestResponse;
import com.ces.erp.request.entity.RequestStatusLog;
import com.ces.erp.request.entity.TechParam;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.RequestStatusLogRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TechRequestService implements ApprovalHandler {

    private final TechRequestRepository requestRepository;
    private final RequestStatusLogRepository statusLogRepository;
    private final CustomerRepository customerRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS;
    static {
        Map<RequestStatus, Set<RequestStatus>> m = new java.util.EnumMap<>(RequestStatus.class);
        m.put(RequestStatus.DRAFT, Set.of(RequestStatus.PENDING, RequestStatus.REJECTED));
        m.put(RequestStatus.PENDING, Set.of(RequestStatus.PM_REVIEW, RequestStatus.REJECTED));
        m.put(RequestStatus.PM_REVIEW, Set.of(RequestStatus.PM_SHORTLIST_READY, RequestStatus.REJECTED));
        m.put(RequestStatus.PM_SHORTLIST_READY, Set.of(RequestStatus.COORDINATOR_NEGOTIATING, RequestStatus.REJECTED));
        m.put(RequestStatus.COORDINATOR_NEGOTIATING, Set.of(RequestStatus.COORDINATOR_PROPOSED, RequestStatus.REJECTED));
        m.put(RequestStatus.COORDINATOR_PROPOSED, Set.of(RequestStatus.PM_PRICE_NEGOTIATION, RequestStatus.REJECTED));
        m.put(RequestStatus.PM_PRICE_NEGOTIATION, Set.of(RequestStatus.PM_APPROVED, RequestStatus.REJECTED));
        m.put(RequestStatus.PM_APPROVED, Set.of(RequestStatus.ACCOUNTING_DOCS_CHECK, RequestStatus.REJECTED));
        m.put(RequestStatus.ACCOUNTING_DOCS_CHECK, Set.of(RequestStatus.EXECUTION_READY, RequestStatus.REJECTED));
        m.put(RequestStatus.EXECUTION_READY, Set.of(RequestStatus.OPERATOR_ASSIGNED, RequestStatus.REJECTED));
        m.put(RequestStatus.OPERATOR_ASSIGNED, Set.of(RequestStatus.EQUIPMENT_DISPATCHED, RequestStatus.REJECTED));
        m.put(RequestStatus.EQUIPMENT_DISPATCHED, Set.of(RequestStatus.DELIVERED, RequestStatus.REJECTED));
        m.put(RequestStatus.DELIVERED, Set.of());
        m.put(RequestStatus.REJECTED, Set.of());
        ALLOWED_TRANSITIONS = java.util.Collections.unmodifiableMap(m);
    }

    @Override public String getEntityType() { return "REQUEST"; }
    @Override public String getModuleCode()  { return "REQUESTS"; }
    @Override public String getLabel(Long id) { return resolveCode(findOrThrow(id)); }
    @Override public Object getSnapshot(Long id) { return TechRequestResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            TechRequestRequest req = objectMapper.readValue(json, TechRequestRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    @Transactional(readOnly = true)
    public List<TechRequestResponse> getAll() {
        return requestRepository.findAllByDeletedFalse().stream()
                .map(TechRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TechRequestResponse> getAllPaged(String search, RequestStatus status,
                                                          String region, String projectType,
                                                          int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return PagedResponse.from(
                requestRepository.findAllFiltered(search, status, region, projectType, pageable),
                TechRequestResponse::from
        );
    }

    @Transactional
    public TechRequestResponse changeStatus(Long id, RequestStatus newStatus, String reason) {
        TechRequest entity = findOrThrow(id);
        RequestStatus oldStatus = entity.getStatus();

        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(oldStatus.name() + " statusundan " + newStatus.name() + " statusuna keçid mümkün deyil");
        }

        entity.setStatus(newStatus);
        TechRequest saved = requestRepository.save(entity);

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";

        statusLogRepository.save(RequestStatusLog.builder()
                .requestId(id)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(username)
                .build());

        String code = resolveCode(saved);
        auditService.log("SORĞU", saved.getId(), code,
                "STATUS_DƏYİŞDİ", oldStatus.name() + " → " + newStatus.name() + (reason != null ? " | " + reason : ""));

        return TechRequestResponse.from(saved);
    }

    public List<StatusLogResponse> getStatusHistory(Long id) {
        findOrThrow(id);
        return statusLogRepository.findAllByRequestIdOrderByChangedAtDesc(id).stream()
                .map(StatusLogResponse::from)
                .toList();
    }

    public Map<String, List<String>> getAllowedTransitions() {
        Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<RequestStatus, Set<RequestStatus>> e : ALLOWED_TRANSITIONS.entrySet()) {
            result.put(e.getKey().name(), e.getValue().stream().map(Enum::name).toList());
        }
        return result;
    }

    @Transactional
    public void bulkUpdateNotes(List<Long> ids, String notes) {
        for (Long id : ids) {
            TechRequest entity = findOrThrow(id);
            entity.setNotes(notes);
            requestRepository.save(entity);
        }
    }

    @Transactional
    public void bulkUpdateRegion(List<Long> ids, String region) {
        for (Long id : ids) {
            TechRequest entity = findOrThrow(id);
            entity.setRegion(region);
            requestRepository.save(entity);
        }
    }

    public TechRequestResponse getById(Long id) {
        return TechRequestResponse.from(findOrThrow(id));
    }

    @Transactional
    public TechRequestResponse create(TechRequestRequest req, Long userId) {
        TechRequest entity = buildEntity(req, new TechRequest());
        entity.setStatus(RequestStatus.PENDING);
        entity.setCreatedBy(userRepository.findById(userId).orElse(null));
        TechRequest saved = requestRepository.save(entity);
        saved.setRequestCode("REQ-" + String.format("%04d", saved.getId()));
        saved = requestRepository.save(saved);
        String code = resolveCode(saved);
        auditService.log("SORĞU", saved.getId(), code, "YARADILDI", "Yeni texniki sorğu yaradıldı");
        return TechRequestResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "REQUESTS", entityType = "REQUEST")
    public TechRequestResponse update(Long id, TechRequestRequest req) {
        TechRequest entity = findOrThrow(id);
        // PM-ə yönləndirildikdən sonra və ya icra prosesində olan sorğular redaktə edilə bilməz
        Set<RequestStatus> editableStatuses = Set.of(RequestStatus.DRAFT, RequestStatus.PENDING);
        if (!editableStatuses.contains(entity.getStatus())) {
            throw new InvalidStatusTransitionException("Bu statusda olan sorğu redaktə edilə bilməz");
        }
        buildEntity(req, entity);
        TechRequest updated = requestRepository.save(entity);
        auditService.log("SORĞU", updated.getId(), resolveCode(updated), "YENİLƏNDİ", "Sorğu yeniləndi");
        return TechRequestResponse.from(updated);
    }

    @Transactional
    public TechRequestResponse submit(Long id) {
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() != RequestStatus.DRAFT) {
            throw new BusinessException("Yalnız DRAFT statuslu sorğu göndərilə bilər");
        }
        RequestStatus oldStatus = entity.getStatus();
        entity.setStatus(RequestStatus.PENDING);
        TechRequest saved = requestRepository.save(entity);
        logStatusChange(saved.getId(), oldStatus, RequestStatus.PENDING, null);
        return TechRequestResponse.from(saved);
    }

    @Transactional
    public TechRequestResponse selectEquipment(Long id, Long equipmentId) {
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Texnika seçmək üçün sorğu PENDING statusunda olmalıdır");
        }
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", equipmentId));
        if (equipment.getStatus() == EquipmentStatus.RENTED) {
            throw new BusinessException("Bu texnika hazırda icarədədir və başqa sorğuya təyin edilə bilməz");
        }
        entity.setSelectedEquipment(equipment);
        return TechRequestResponse.from(requestRepository.save(entity));
    }

    @Transactional
    public TechRequestResponse sendToCoordinator(Long id) {
        // Yeni flowda sorğu birbaşa Koordinatora yox, Layihə Menecerinə yönləndirilir.
        // Bu metod backward-compat üçün saxlanılır — eyni effekti (PENDING → PM_REVIEW) verir.
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Yönləndirmək üçün sorğu PENDING statusunda olmalıdır");
        }
        RequestStatus oldStatus = entity.getStatus();
        entity.setStatus(RequestStatus.PM_REVIEW);
        TechRequest saved = requestRepository.save(entity);
        logStatusChange(saved.getId(), oldStatus, RequestStatus.PM_REVIEW, null);
        return TechRequestResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "REQUESTS", entityType = "REQUEST", isDelete = true)
    public void delete(Long id) {
        TechRequest entity = findOrThrow(id);
        auditService.log("SORĞU", entity.getId(), resolveCode(entity), "SİLİNDİ", "Sorğu silindi");
        entity.softDelete();
        requestRepository.save(entity);
    }

    private TechRequest findOrThrow(Long id) {
        return requestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", id));
    }

    private TechRequest buildEntity(TechRequestRequest req, TechRequest entity) {
        if (req.getCustomerId() != null) {
            entity.setCustomer(customerRepository.findByIdAndDeletedFalse(req.getCustomerId()).orElse(null));
        } else {
            Customer customer = customerRepository
                    .findByCompanyNameIgnoreCaseAndDeletedFalse(req.getCompanyName())
                    .orElseGet(() -> {
                        Customer newCustomer = new Customer();
                        newCustomer.setCompanyName(req.getCompanyName());
                        newCustomer.setOfficeContactPerson(req.getContactPerson());
                        newCustomer.setOfficeContactPhone(req.getContactPhone());
                        newCustomer.setStatus(CustomerStatus.ACTIVE);
                        newCustomer.setRiskLevel(RiskLevel.LOW);
                        return customerRepository.save(newCustomer);
                    });
            entity.setCustomer(customer);
        }
        entity.setCompanyName(req.getCompanyName());
        entity.setContactPerson(req.getContactPerson());
        entity.setContactPhone(req.getContactPhone());
        entity.setProjectName(req.getProjectName());
        entity.setRegion(req.getRegion());
        entity.setRequestDate(req.getRequestDate());
        entity.setProjectType(req.getProjectType());
        entity.setDayCount(req.getDayCount());
        entity.setTransportationRequired(req.isTransportationRequired());
        entity.setNotes(req.getNotes());

        List<TechParam> params = req.getParams() == null ? List.of() :
                req.getParams().stream()
                        .filter(p -> p.getParamKey() != null && !p.getParamKey().isBlank())
                        .map(p -> new TechParam(p.getParamKey(), p.getParamValue()))
                        .toList();
        entity.getParams().clear();
        entity.getParams().addAll(params);

        if (req.getSelectedEquipmentId() != null) {
            entity.setSelectedEquipment(equipmentRepository.findById(req.getSelectedEquipmentId()).orElse(null));
        }

        return entity;
    }

    private String resolveCode(TechRequest entity) {
        return entity.getRequestCode() != null ? entity.getRequestCode()
                : "REQ-" + String.format("%04d", entity.getId());
    }

    private void logStatusChange(Long requestId, RequestStatus oldStatus, RequestStatus newStatus, String reason) {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        statusLogRepository.save(RequestStatusLog.builder()
                .requestId(requestId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(username)
                .build());
    }
}
