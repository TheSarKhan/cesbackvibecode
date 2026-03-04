package com.ces.erp.department.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.department.dto.DepartmentRequest;
import com.ces.erp.department.dto.DepartmentResponse;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAllByDeletedFalse().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    public DepartmentResponse getById(Long id) {
        Department dept = departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id));
        return DepartmentResponse.from(dept);
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new BusinessException("Bu adda şöbə artıq mövcuddur");
        }
        Department dept = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return DepartmentResponse.from(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department dept = departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id));
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        return DepartmentResponse.from(departmentRepository.save(dept));
    }

    @Transactional
    public void delete(Long id) {
        Department dept = departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id));
        dept.softDelete();
        departmentRepository.save(dept);
    }
}
