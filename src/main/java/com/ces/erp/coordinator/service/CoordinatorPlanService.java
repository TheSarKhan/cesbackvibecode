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
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.config.repository.ConfigItemRepository;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.entity.EquipmentDocument;
import com.ces.erp.garage.repository.EquipmentDocumentRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

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

    private static final List<RequestStatus> COORDINATOR_STATUSES = List.of(
            RequestStatus.SENT_TO_COORDINATOR,
            RequestStatus.OFFER_SENT,
            RequestStatus.ACCEPTED,
            RequestStatus.REJECTED
    );

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

    @Transactional(readOnly = true)
    public PagedResponse<CoordinatorPlanResponse> getRequestsPaged(int page, int size, String search, String status) {
        String q = (search != null && !search.isBlank()) ? search : null;
        RequestStatus s = null;
        if (status != null && !status.isBlank()) {
            try { s = RequestStatus.valueOf(status); } catch (IllegalArgumentException ignored) { }
        }
        // If a specific status is requested but it's not a coordinator status, return empty
        if (s != null && !COORDINATOR_STATUSES.contains(s)) {
            s = null;
        }
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
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
        return planRepository.findByRequestId(requestId)
                .map(CoordinatorPlanResponse::from)
                .orElseGet(() -> CoordinatorPlanResponse.fromRequest(request));
    }

    @Transactional
    public CoordinatorPlanResponse savePlan(Long requestId, CoordinatorPlanRequest req, Long userId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (!COORDINATOR_STATUSES.contains(request.getStatus())) {
            throw new BusinessException("Bu sorğu koordinator üçün uyğun deyil");
        }

        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseGet(() -> CoordinatorPlan.builder().request(request).build());

        if (req.getOperatorId() != null) {
            var operator = operatorRepository.findByIdActive(req.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator", req.getOperatorId()));
            boolean busy = planRepository.isOperatorBusyInOtherProject(
                    req.getOperatorId(), requestId,
                    List.of(ProjectStatus.PENDING, ProjectStatus.ACTIVE));
            if (busy) {
                throw new BusinessException("Bu operator artıq başqa aktiv layihəyə təyin edilib");
            }
            plan.setOperator(operator);
        } else {
            plan.setOperator(null);
        }
        plan.setDayCount(req.getDayCount());
        plan.setEquipmentPrice(req.getEquipmentPrice());
        plan.setContractorPayment(req.getContractorPayment());
        plan.setOperatorPayment(req.getOperatorPayment());
        plan.setTransportationPrice(req.getTransportationPrice());
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        if (req.getSafetyEquipmentIds() != null) {
            plan.getSafetyEquipment().clear();
            plan.getSafetyEquipment().addAll(configItemRepository.findAllById(req.getSafetyEquipmentIds()));
        }
        plan.setNotes(req.getNotes());

        return CoordinatorPlanResponse.from(planRepository.save(plan));
    }

    public void validateBeforeSubmit(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.SENT_TO_COORDINATOR) {
            throw new BusinessException("Plan yalnız SENT_TO_COORDINATOR statusunda göndərilə bilər");
        }
        CoordinatorPlan existing = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Əvvəlcə koordinator planını doldurun"));
        if (existing.getSelectedEquipment() == null) {
            throw new BusinessException("Texnika seçilməlidir");
        }
        if (existing.getEquipmentPrice() == null || existing.getEquipmentPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Texnika qiyməti daxil edilməlidir");
        }
        if (existing.getStartDate() == null || existing.getEndDate() == null) {
            throw new BusinessException("Başlanğıc və bitmə tarixi daxil edilməlidir");
        }
        if (existing.getEndDate().isBefore(existing.getStartDate())) {
            throw new BusinessException("Bitmə tarixi başlanğıc tarixindən əvvəl ola bilməz");
        }
    }

    @Transactional
    @RequiresApproval(module = "COORDINATOR", entityType = "COORDINATOR_SUBMIT")
    public CoordinatorPlanResponse submitPlan(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        request.setStatus(RequestStatus.OFFER_SENT);
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

    @Transactional
    public void acceptOffer(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.OFFER_SENT) {
            throw new BusinessException("Təklif yalnız OFFER_SENT statusunda qəbul edilə bilər");
        }
        request.setStatus(RequestStatus.ACCEPTED);
        requestRepository.save(request);

        // Layihə artıq yaradılmayıbsa — PENDING layihə yarat
        if (!projectRepository.existsByRequestIdAndDeletedFalse(requestId)) {
            int nextNum = projectRepository.findMaxProjectCodeNumber() + 1;
            Project project = Project.builder()
                    .projectCode("PRJ-" + String.format("%04d", nextNum))
                    .request(request)
                    .status(ProjectStatus.PENDING)
                    .build();
            projectRepository.save(project);
        }
    }

    @Transactional
    public void rejectOffer(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        RequestStatus currentStatus = request.getStatus();
        if (currentStatus != RequestStatus.OFFER_SENT && currentStatus != RequestStatus.SENT_TO_COORDINATOR) {
            throw new BusinessException("Sorğu yalnız OFFER_SENT və ya SENT_TO_COORDINATOR statusunda rədd edilə bilər");
        }
        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);

        // Texnikanı yenidən Mövcud et — yalnız OFFER_SENT statusunda texnika İcarədə sayılırdı
        if (currentStatus == RequestStatus.OFFER_SENT) {
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
