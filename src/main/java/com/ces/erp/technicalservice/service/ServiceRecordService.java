package com.ces.erp.technicalservice.service;

import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.garage.service.EquipmentService;
import com.ces.erp.technicalservice.dto.ServiceChecklistItemDto;
import com.ces.erp.technicalservice.dto.ServiceRecordRequest;
import com.ces.erp.technicalservice.dto.ServiceRecordResponse;
import com.ces.erp.technicalservice.entity.ServiceChecklistItem;
import com.ces.erp.technicalservice.entity.ServiceRecord;
import com.ces.erp.technicalservice.entity.ServiceRecordType;
import com.ces.erp.technicalservice.repository.ServiceChecklistItemRepository;
import com.ces.erp.technicalservice.repository.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRecordService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final ServiceChecklistItemRepository checklistItemRepository;
    private final EquipmentRepository equipmentRepository;
    private final ContractorRepository contractorRepository;
    private final EquipmentService equipmentService;

    @Transactional(readOnly = true)
    public List<ServiceRecordResponse> getAll(ServiceRecordType recordType) {
        List<ServiceRecord> records = (recordType != null)
                ? serviceRecordRepository.findAllActiveByType(recordType)
                : serviceRecordRepository.findAllActive();
        return records.stream().map(ServiceRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceRecordResponse getById(Long id) {
        return ServiceRecordResponse.from(findOrThrow(id));
    }

    @Transactional
    public ServiceRecordResponse create(ServiceRecordRequest request, Long userId) {
        Equipment equipment = equipmentRepository.findByIdAndDeletedFalse(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Texnika", request.getEquipmentId()));

        EquipmentStatus statusBefore = equipment.getStatus();

        ServiceRecord record = ServiceRecord.builder()
                .equipment(equipment)
                .contractor(request.getContractorId() != null 
                        ? contractorRepository.findByIdAndDeletedFalse(request.getContractorId()).orElse(null) 
                        : null)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .serviceDate(request.getServiceDate())
                .nextServiceDate(request.getNextServiceDate())
                .cost(request.getCost())
                .odometer(request.getOdometer())
                .notes(request.getNotes())
                .statusBefore(statusBefore)
                .statusAfter(request.getStatusAfter())
                .build();

        if (request.getChecklistItems() != null) {
            record.setChecklistItems(request.getChecklistItems().stream()
                    .map(dto -> ServiceChecklistItem.builder()
                            .serviceRecord(record)
                            .itemName(dto.getItemName())
                            .checked(dto.isChecked())
                            .note(dto.getNote())
                            .build())
                    .collect(Collectors.toList()));
        }

        record.setRecordType(request.getRecordType());

        // Eyni texnika üçün açıq qeyd yoxlanışı
        if (request.getRecordType() != null) {
            boolean hasOpen = serviceRecordRepository.existsOpenByEquipmentAndType(
                    request.getEquipmentId(), request.getRecordType());
            if (hasOpen) {
                String label = request.getRecordType() == ServiceRecordType.INSPECTION ? "baxış" : "servis";
                throw new com.ces.erp.common.exception.BusinessException(
                        "Bu texnika üçün artıq açıq " + label + " qeydi mövcuddur");
            }
        }

        // Status keçidi: qeyd tipinə görə
        if (request.getRecordType() == ServiceRecordType.INSPECTION) {
            if (statusBefore == EquipmentStatus.IN_TRANSIT || statusBefore == EquipmentStatus.AVAILABLE) {
                equipmentService.updateStatus(equipment.getId(), EquipmentStatus.IN_INSPECTION.name(),
                        "Texniki baxışa qəbul edildi", userId);
            }
        } else if (request.getRecordType() == ServiceRecordType.REPAIR) {
            if (statusBefore == EquipmentStatus.DEFECTIVE) {
                equipmentService.updateStatus(equipment.getId(), EquipmentStatus.IN_REPAIR.name(),
                        "Texniki servisə alındı: " + request.getServiceType(), userId);
            }
        } else {
            // Köhnə davranış (recordType olmayan qeydlər üçün)
            if (statusBefore == EquipmentStatus.IN_TRANSIT || statusBefore == EquipmentStatus.AVAILABLE) {
                equipmentService.updateStatus(equipment.getId(), EquipmentStatus.IN_INSPECTION.name(),
                        "Texnika servisdə qəbul edildi: " + request.getServiceType(), userId);
            }
        }

        return ServiceRecordResponse.from(serviceRecordRepository.save(record));
    }

    @Transactional
    public ServiceRecordResponse update(Long id, ServiceRecordRequest request) {
        ServiceRecord record = findOrThrow(id);
        
        record.setServiceType(request.getServiceType());
        record.setDescription(request.getDescription());
        record.setServiceDate(request.getServiceDate());
        record.setNextServiceDate(request.getNextServiceDate());
        record.setCost(request.getCost());
        record.setOdometer(request.getOdometer());
        record.setNotes(request.getNotes());
        record.setStatusAfter(request.getStatusAfter());
        
        if (request.getContractorId() != null) {
            record.setContractor(contractorRepository.findByIdAndDeletedFalse(request.getContractorId()).orElse(null));
        }

        if (request.getChecklistItems() != null) {
            record.getChecklistItems().clear();
            record.getChecklistItems().addAll(request.getChecklistItems().stream()
                    .map(dto -> ServiceChecklistItem.builder()
                            .serviceRecord(record)
                            .itemName(dto.getItemName())
                            .checked(dto.isChecked())
                            .note(dto.getNote())
                            .build())
                    .toList());
        }

        return ServiceRecordResponse.from(serviceRecordRepository.save(record));
    }

    @Transactional
    public ServiceRecordResponse complete(Long id, EquipmentStatus statusAfter, BigDecimal cost, Long userId) {
        ServiceRecord record = findOrThrow(id);
        record.setCompleted(true);
        record.setStatusAfter(statusAfter);
        if (cost != null) {
            record.setCost(cost);
        }

        // Texnikanın statusunu servisin nəticəsinə görə yenilə (UNDER_CHECK, AVAILABLE, DEFECTIVE və s.)
        if (statusAfter != null) {
            equipmentService.updateStatus(record.getEquipment().getId(), statusAfter.name(),
                    "Servis tamamlandı: " + record.getServiceType(), userId);

            // Texnikanın son baxış tarixini yenilə
            Equipment eq = record.getEquipment();
            eq.setLastInspectionDate(record.getServiceDate());
            equipmentRepository.save(eq);
        }

        return ServiceRecordResponse.from(serviceRecordRepository.save(record));
    }

    @Transactional
    public ServiceRecordResponse updateChecklistItem(Long recordId, Long itemId, boolean checked, String note) {
        ServiceRecord record = findOrThrow(recordId);
        ServiceChecklistItem item = record.getChecklistItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Checklist maddəsi", itemId));
        item.setChecked(checked);
        if (note != null) item.setNote(note);
        return ServiceRecordResponse.from(serviceRecordRepository.save(record));
    }

    @Transactional
    public void delete(Long id) {
        ServiceRecord record = findOrThrow(id);
        record.softDelete();
        serviceRecordRepository.save(record);
    }

    private ServiceRecord findOrThrow(Long id) {
        return serviceRecordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Servis qeydi", id));
    }
}
