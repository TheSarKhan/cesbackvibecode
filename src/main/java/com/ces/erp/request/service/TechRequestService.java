package com.ces.erp.request.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.request.dto.TechRequestRequest;
import com.ces.erp.request.dto.TechRequestResponse;
import com.ces.erp.request.entity.TechParam;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechRequestService implements ApprovalHandler {

    private final TechRequestRepository requestRepository;
    private final CustomerRepository customerRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "REQUEST"; }
    @Override public String getModuleCode()  { return "REQUESTS"; }
    @Override public String getLabel(Long id) { return findOrThrow(id).getRequestCode(); }
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

    public List<TechRequestResponse> getAll() {
        return requestRepository.findAllByDeletedFalse().stream()
                .map(TechRequestResponse::from)
                .toList();
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
        return TechRequestResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "REQUESTS", entityType = "REQUEST")
    public TechRequestResponse update(Long id, TechRequestRequest req) {
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() == RequestStatus.SENT_TO_COORDINATOR
                || entity.getStatus() == RequestStatus.OFFER_SENT
                || entity.getStatus() == RequestStatus.ACCEPTED) {
            throw new BusinessException("Bu statusda olan sorğu redaktə edilə bilməz");
        }
        buildEntity(req, entity);
        return TechRequestResponse.from(requestRepository.save(entity));
    }

    @Transactional
    public TechRequestResponse submit(Long id) {
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() != RequestStatus.DRAFT) {
            throw new BusinessException("Yalnız DRAFT statuslu sorğu göndərilə bilər");
        }
        entity.setStatus(RequestStatus.PENDING);
        return TechRequestResponse.from(requestRepository.save(entity));
    }

    @Transactional
    public TechRequestResponse selectEquipment(Long id, Long equipmentId) {
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Texnika seçmək üçün sorğu PENDING statusunda olmalıdır");
        }
        entity.setSelectedEquipment(equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", equipmentId)));
        return TechRequestResponse.from(requestRepository.save(entity));
    }

    @Transactional
    public TechRequestResponse sendToCoordinator(Long id) {
        TechRequest entity = findOrThrow(id);
        if (entity.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Koordinatora göndərmək üçün sorğu PENDING statusunda olmalıdır");
        }
        entity.setStatus(RequestStatus.SENT_TO_COORDINATOR);
        return TechRequestResponse.from(requestRepository.save(entity));
    }

    @Transactional
    @RequiresApproval(module = "REQUESTS", entityType = "REQUEST", isDelete = true)
    public void delete(Long id) {
        TechRequest entity = findOrThrow(id);
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
            entity.setCustomer(null);
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
}
