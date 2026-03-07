package com.ces.erp.coordinator.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.coordinator.dto.CoordinatorPlanRequest;
import com.ces.erp.coordinator.dto.CoordinatorPlanResponse;
import com.ces.erp.coordinator.entity.CoordinatorDocument;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorDocumentRepository;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.repository.EquipmentRepository;
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
public class CoordinatorPlanService {

    private final TechRequestRepository requestRepository;
    private final CoordinatorPlanRepository planRepository;
    private final CoordinatorDocumentRepository documentRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProjectRepository projectRepository;

    private static final List<RequestStatus> COORDINATOR_STATUSES = List.of(
            RequestStatus.SENT_TO_COORDINATOR,
            RequestStatus.OFFER_SENT,
            RequestStatus.ACCEPTED,
            RequestStatus.REJECTED
    );

    public List<CoordinatorPlanResponse> getRequests() {
        return requestRepository.findAllByStatusInAndDeletedFalse(COORDINATOR_STATUSES).stream()
                .map(r -> planRepository.findByRequestId(r.getId())
                        .map(CoordinatorPlanResponse::from)
                        .orElseGet(() -> CoordinatorPlanResponse.fromRequest(r)))
                .toList();
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

        plan.setOperatorName(req.getOperatorName());
        plan.setEquipmentPrice(req.getEquipmentPrice());
        plan.setContractorPayment(req.getContractorPayment());
        plan.setTransportationPrice(req.getTransportationPrice());
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        plan.setHasFlashingLights(req.isHasFlashingLights());
        plan.setHasFireExtinguisher(req.isHasFireExtinguisher());
        plan.setHasFirstAid(req.isHasFirstAid());
        plan.setNotes(req.getNotes());

        return CoordinatorPlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public CoordinatorPlanResponse submitPlan(Long requestId) {
        TechRequest request = findRequestOrThrow(requestId);
        if (request.getStatus() != RequestStatus.SENT_TO_COORDINATOR) {
            throw new BusinessException("Plan yalnız SENT_TO_COORDINATOR statusunda göndərilə bilər");
        }
        if (!planRepository.existsByRequestIdAndDeletedFalse(requestId)) {
            throw new BusinessException("Əvvəlcə koordinator planını doldurun");
        }
        request.setStatus(RequestStatus.OFFER_SENT);
        requestRepository.save(request);

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
        if (request.getStatus() != RequestStatus.OFFER_SENT) {
            throw new BusinessException("Təklif yalnız OFFER_SENT statusunda rədd edilə bilər");
        }
        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);
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

        plan.setSelectedEquipment(equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", equipmentId)));

        return CoordinatorPlanResponse.from(planRepository.save(plan));
    }

    // ─── Sənədlər ─────────────────────────────────────────────────────────────

    @Transactional
    public CoordinatorPlanResponse.DocumentDto uploadDocument(Long requestId, MultipartFile file,
                                                               String documentName, String documentType,
                                                               Long userId) {
        CoordinatorPlan plan = planRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException("Əvvəlcə koordinator planını yadda saxlayın"));

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
