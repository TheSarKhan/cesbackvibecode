package com.ces.erp.hr.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.hr.dto.PositionRequest;
import com.ces.erp.hr.dto.PositionResponse;
import com.ces.erp.hr.entity.Position;
import com.ces.erp.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public List<PositionResponse> getAll() {
        return positionRepository.findAllByDeletedFalseOrderByNameAsc().stream()
                .map(PositionResponse::from)
                .toList();
    }

    public PositionResponse getById(Long id) {
        return PositionResponse.from(loadActive(id));
    }

    @Transactional
    public PositionResponse create(PositionRequest req) {
        if (positionRepository.existsByNameAndDeletedFalse(req.getName())) {
            throw new DuplicateResourceException("Bu adda vəzifə artıq mövcuddur");
        }
        Department dept = req.getDepartmentId() != null
                ? departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Şöbə", req.getDepartmentId()))
                : null;

        Position p = Position.builder()
                .name(req.getName())
                .description(req.getDescription())
                .defaultSalary(req.getDefaultSalary())
                .department(dept)
                .active(req.getActive() == null || req.getActive())
                .build();
        Position saved = positionRepository.save(p);
        auditService.log("HR_VƏZİFƏ", saved.getId(), saved.getName(), "YARADILDI", "Yeni vəzifə yaradıldı");
        return PositionResponse.from(saved);
    }

    @Transactional
    public PositionResponse update(Long id, PositionRequest req) {
        Position p = loadActive(id);
        Department dept = req.getDepartmentId() != null
                ? departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Şöbə", req.getDepartmentId()))
                : null;

        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setDefaultSalary(req.getDefaultSalary());
        p.setDepartment(dept);
        if (req.getActive() != null) p.setActive(req.getActive());
        Position saved = positionRepository.save(p);
        auditService.log("HR_VƏZİFƏ", saved.getId(), saved.getName(), "YENİLƏNDİ", "Vəzifə yeniləndi");
        return PositionResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Position p = loadActive(id);
        p.softDelete();
        positionRepository.save(p);
        auditService.log("HR_VƏZİFƏ", p.getId(), p.getName(), "SİLİNDİ", "Vəzifə silindi");
    }

    private Position loadActive(Long id) {
        return positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vəzifə", id));
    }
}
