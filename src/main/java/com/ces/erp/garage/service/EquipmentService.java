package com.ces.erp.garage.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.garage.dto.*;
import com.ces.erp.garage.entity.*;
import com.ces.erp.garage.repository.*;
import com.ces.erp.enums.OwnershipType;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentInspectionRepository inspectionRepository;
    private final EquipmentDocumentRepository documentRepository;
    private final EquipmentProjectHistoryRepository projectHistoryRepository;
    private final ContractorRepository contractorRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getAll() {
        return equipmentRepository.findAllByDeletedFalse().stream()
                .map(EquipmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getById(Long id) {
        return EquipmentResponse.from(findWithDetails(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectHistoryResponse> getProjectHistory(Long id) {
        findOrThrow(id);
        return projectHistoryRepository.findAllByEquipmentIdOrderByStartDateDesc(id).stream()
                .map(ProjectHistoryResponse::from)
                .toList();
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        validateCodes(request, null);
        Equipment equipment = buildEquipment(request, new Equipment());
        return EquipmentResponse.from(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = findOrThrow(id);
        validateCodes(request, id);
        return EquipmentResponse.from(equipmentRepository.save(buildEquipment(request, equipment)));
    }

    @Transactional
    public void delete(Long id) {
        Equipment equipment = findOrThrow(id);
        equipment.softDelete();
        equipmentRepository.save(equipment);
    }

    // ─── Texniki baxış ────────────────────────────────────────────────────────

    @Transactional
    public InspectionResponse addInspection(Long equipmentId, InspectionRequest request, MultipartFile document) {
        Equipment equipment = findOrThrow(equipmentId);

        EquipmentInspection inspection = EquipmentInspection.builder()
                .equipment(equipment)
                .inspectionDate(request.getInspectionDate())
                .description(request.getDescription())
                .performedBy(request.getPerformedByUserId() != null
                        ? userRepository.findByIdAndDeletedFalse(request.getPerformedByUserId())
                                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", request.getPerformedByUserId()))
                        : null)
                .build();

        if (document != null && !document.isEmpty()) {
            String path = fileStorageService.store(document, "inspections");
            inspection.setDocumentPath(path);
            inspection.setDocumentName(document.getOriginalFilename());
        }

        return InspectionResponse.from(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionResponse uploadInspectionDocument(Long equipmentId, Long inspectionId, MultipartFile file) {
        EquipmentInspection inspection = inspectionRepository
                .findByIdAndEquipmentId(inspectionId, equipmentId)
                .orElseThrow(() -> new BusinessException("Texniki baxış tapılmadı"));
        if (inspection.getDocumentPath() != null) {
            fileStorageService.delete(inspection.getDocumentPath());
        }
        String path = fileStorageService.store(file, "inspections");
        inspection.setDocumentPath(path);
        inspection.setDocumentName(file.getOriginalFilename());
        return InspectionResponse.from(inspectionRepository.save(inspection));
    }

    @Transactional
    public void deleteInspection(Long equipmentId, Long inspectionId) {
        EquipmentInspection inspection = inspectionRepository
                .findByIdAndEquipmentId(inspectionId, equipmentId)
                .orElseThrow(() -> new BusinessException("Texniki baxış tapılmadı"));
        if (inspection.getDocumentPath() != null) {
            fileStorageService.delete(inspection.getDocumentPath());
        }
        inspectionRepository.delete(inspection);
    }

    // ─── Sənəd ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponse uploadDocument(Long equipmentId, MultipartFile file, Long userId) {
        Equipment equipment = findOrThrow(equipmentId);

        String path = fileStorageService.store(file, "equipment-docs");

        User uploader = userId != null
                ? userRepository.findByIdAndDeletedFalse(userId).orElse(null)
                : null;

        EquipmentDocument doc = EquipmentDocument.builder()
                .equipment(equipment)
                .documentName(file.getOriginalFilename())
                .filePath(path)
                .fileType(file.getContentType())
                .uploadedBy(uploader)
                .build();

        return DocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Long equipmentId, Long documentId) {
        EquipmentDocument doc = documentRepository
                .findByIdAndEquipmentId(documentId, equipmentId)
                .orElseThrow(() -> new BusinessException("Sənəd tapılmadı"));
        fileStorageService.delete(doc.getFilePath());
        documentRepository.delete(doc);
    }

    public java.nio.file.Path resolveDocumentPath(Long equipmentId, Long documentId) {
        EquipmentDocument doc = documentRepository
                .findByIdAndEquipmentId(documentId, equipmentId)
                .orElseThrow(() -> new BusinessException("Sənəd tapılmadı"));
        return fileStorageService.resolve(doc.getFilePath());
    }

    public java.nio.file.Path resolveInspectionDocPath(Long equipmentId, Long inspectionId) {
        EquipmentInspection inspection = inspectionRepository
                .findByIdAndEquipmentId(inspectionId, equipmentId)
                .orElseThrow(() -> new BusinessException("Texniki baxış tapılmadı"));
        if (inspection.getDocumentPath() == null) {
            throw new BusinessException("Bu baxışa aid sənəd yoxdur");
        }
        return fileStorageService.resolve(inspection.getDocumentPath());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Equipment buildEquipment(EquipmentRequest r, Equipment e) {
        e.setEquipmentCode(r.getEquipmentCode());
        e.setName(r.getName());
        e.setType(r.getType());
        e.setSerialNumber(r.getSerialNumber());
        e.setBrand(r.getBrand());
        e.setModel(r.getModel());
        e.setManufactureYear(r.getManufactureYear());
        e.setPurchaseDate(r.getPurchaseDate());
        e.setPurchasePrice(r.getPurchasePrice());
        e.setPlateNumber(r.getPlateNumber());
        e.setWeightTon(r.getWeightTon());
        e.setCurrentMarketValue(r.getCurrentMarketValue());
        e.setDepreciationRate(r.getDepreciationRate());
        e.setHourKmCounter(r.getHourKmCounter());
        e.setMotoHours(r.getMotoHours());
        e.setStorageLocation(r.getStorageLocation());
        e.setStatus(r.getStatus());
        e.setRepairStatus(r.getRepairStatus());
        e.setNotes(r.getNotes());
        e.setOwnershipType(r.getOwnershipType());

        if (r.getResponsibleUserId() != null) {
            e.setResponsibleUser(userRepository.findByIdAndDeletedFalse(r.getResponsibleUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", r.getResponsibleUserId())));
        } else {
            e.setResponsibleUser(null);
        }

        if (r.getOwnershipType() == OwnershipType.CONTRACTOR) {
            if (r.getOwnerContractorId() == null) {
                throw new BusinessException("Podratçı texnikası üçün podratçı ID məcburidir");
            }
            Contractor contractor = contractorRepository.findByIdAndDeletedFalse(r.getOwnerContractorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Podratçı", r.getOwnerContractorId()));
            e.setOwnerContractor(contractor);
            e.setOwnerInvestorName(null);
            e.setOwnerInvestorVoen(null);
            e.setOwnerInvestorPhone(null);
        } else if (r.getOwnershipType() == OwnershipType.INVESTOR) {
            e.setOwnerContractor(null);
            e.setOwnerInvestorName(r.getOwnerInvestorName());
            e.setOwnerInvestorVoen(r.getOwnerInvestorVoen());
            e.setOwnerInvestorPhone(r.getOwnerInvestorPhone());
        } else {
            e.setOwnerContractor(null);
            e.setOwnerInvestorName(null);
            e.setOwnerInvestorVoen(null);
            e.setOwnerInvestorPhone(null);
        }

        return e;
    }

    private void validateCodes(EquipmentRequest request, Long excludeId) {
        if (excludeId == null) {
            if (equipmentRepository.existsByEquipmentCodeAndDeletedFalse(request.getEquipmentCode())) {
                throw new BusinessException("Bu texnika kodu artıq mövcuddur");
            }
            if (request.getSerialNumber() != null &&
                    equipmentRepository.existsBySerialNumberAndDeletedFalse(request.getSerialNumber())) {
                throw new BusinessException("Bu seriya nömrəsi artıq mövcuddur");
            }
        } else {
            if (equipmentRepository.existsByEquipmentCodeAndIdNotAndDeletedFalse(request.getEquipmentCode(), excludeId)) {
                throw new BusinessException("Bu texnika kodu artıq mövcuddur");
            }
            if (request.getSerialNumber() != null &&
                    equipmentRepository.existsBySerialNumberAndIdNotAndDeletedFalse(request.getSerialNumber(), excludeId)) {
                throw new BusinessException("Bu seriya nömrəsi artıq mövcuddur");
            }
        }
    }

    private Equipment findOrThrow(Long id) {
        return equipmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", id));
    }

    private Equipment findWithDetails(Long id) {
        return equipmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", id));
    }
}
