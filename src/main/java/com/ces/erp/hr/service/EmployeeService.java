package com.ces.erp.hr.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.hr.dto.EmployeeRequest;
import com.ces.erp.hr.dto.EmployeeResponse;
import com.ces.erp.hr.entity.Employee;
import com.ces.erp.hr.entity.Position;
import com.ces.erp.hr.repository.EmployeeRepository;
import com.ces.erp.hr.repository.PositionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService implements ApprovalHandler {

    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "EMPLOYEE"; }
    @Override public String getModuleCode()  { return "HR"; }
    @Override public String getLabel(Long id) { return loadActive(id).getFullName(); }
    @Override public Object getSnapshot(Long id) { return EmployeeResponse.from(loadActive(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            EmployeeRequest req = objectMapper.readValue(json, EmployeeRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAllByDeletedFalseOrderByLastNameAsc().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    public PagedResponse<EmployeeResponse> getPaged(int page, int size, String q,
                                                    EmployeeStatus status, Long departmentId, Long positionId) {
        Page<Employee> result = employeeRepository.searchPaged(
                isBlank(q) ? null : q.trim(),
                status, departmentId, positionId,
                PageRequest.of(page, size));
        return PagedResponse.<EmployeeResponse>builder()
                .content(result.getContent().stream().map(EmployeeResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(loadActive(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest req) {
        if (req.getFin() != null && !req.getFin().isBlank()
                && employeeRepository.existsByFinAndDeletedFalse(req.getFin())) {
            throw new DuplicateResourceException("Bu FIN-lə işçi artıq mövcuddur");
        }

        Position pos = req.getPositionId() != null
                ? positionRepository.findByIdAndDeletedFalse(req.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vəzifə", req.getPositionId()))
                : null;
        Department dept = req.getDepartmentId() != null
                ? departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Şöbə", req.getDepartmentId()))
                : null;

        Employee e = Employee.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .fatherName(req.getFatherName())
                .fin(emptyToNull(req.getFin()))
                .idCardSeries(req.getIdCardSeries())
                .idCardNumber(req.getIdCardNumber())
                .gender(req.getGender())
                .birthDate(req.getBirthDate())
                .phone(req.getPhone())
                .email(req.getEmail())
                .address(req.getAddress())
                .position(pos)
                .department(dept)
                .grossSalary(req.getGrossSalary())
                .hireDate(req.getHireDate() != null ? req.getHireDate() : LocalDate.now())
                .terminationDate(req.getTerminationDate())
                .terminationReason(req.getTerminationReason())
                .status(req.getStatus() != null ? req.getStatus() : EmployeeStatus.ACTIVE)
                .bankName(req.getBankName())
                .bankAccount(req.getBankAccount())
                .photoUrl(req.getPhotoUrl())
                .notes(req.getNotes())
                .annualLeaveDays(req.getAnnualLeaveDays() != null ? req.getAnnualLeaveDays() : 21)
                .build();

        e.setEmployeeCode(generateCode());
        Employee saved = employeeRepository.save(e);
        auditService.log("HR_İŞÇİ", saved.getId(), saved.getFullName(), "YARADILDI", "Yeni işçi əlavə edildi");
        return EmployeeResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "HR", entityType = "EMPLOYEE")
    public EmployeeResponse update(Long id, EmployeeRequest req) {
        Employee e = loadActive(id);

        if (req.getFin() != null && !req.getFin().isBlank()
                && !req.getFin().equals(e.getFin())
                && employeeRepository.existsByFinAndDeletedFalse(req.getFin())) {
            throw new DuplicateResourceException("Bu FIN-lə başqa işçi mövcuddur");
        }

        Position pos = req.getPositionId() != null
                ? positionRepository.findByIdAndDeletedFalse(req.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vəzifə", req.getPositionId()))
                : null;
        Department dept = req.getDepartmentId() != null
                ? departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Şöbə", req.getDepartmentId()))
                : null;

        e.setFirstName(req.getFirstName());
        e.setLastName(req.getLastName());
        e.setFatherName(req.getFatherName());
        e.setFin(emptyToNull(req.getFin()));
        e.setIdCardSeries(req.getIdCardSeries());
        e.setIdCardNumber(req.getIdCardNumber());
        e.setGender(req.getGender());
        e.setBirthDate(req.getBirthDate());
        e.setPhone(req.getPhone());
        e.setEmail(req.getEmail());
        e.setAddress(req.getAddress());
        e.setPosition(pos);
        e.setDepartment(dept);
        e.setGrossSalary(req.getGrossSalary());
        if (req.getHireDate() != null) e.setHireDate(req.getHireDate());
        e.setTerminationDate(req.getTerminationDate());
        e.setTerminationReason(req.getTerminationReason());
        if (req.getStatus() != null) e.setStatus(req.getStatus());
        e.setBankName(req.getBankName());
        e.setBankAccount(req.getBankAccount());
        if (req.getPhotoUrl() != null) e.setPhotoUrl(req.getPhotoUrl());
        e.setNotes(req.getNotes());
        if (req.getAnnualLeaveDays() != null) e.setAnnualLeaveDays(req.getAnnualLeaveDays());

        Employee saved = employeeRepository.save(e);
        auditService.log("HR_İŞÇİ", saved.getId(), saved.getFullName(), "YENİLƏNDİ", "İşçi məlumatı yeniləndi");
        return EmployeeResponse.from(saved);
    }

    @Transactional
    public EmployeeResponse terminate(Long id, LocalDate terminationDate, String reason) {
        Employee e = loadActive(id);
        if (e.getStatus() == EmployeeStatus.TERMINATED) {
            throw new BusinessException("Bu işçi onsuz da işdən çıxarılıb");
        }
        e.setStatus(EmployeeStatus.TERMINATED);
        e.setTerminationDate(terminationDate != null ? terminationDate : LocalDate.now());
        e.setTerminationReason(reason);
        Employee saved = employeeRepository.save(e);
        auditService.log("HR_İŞÇİ", saved.getId(), saved.getFullName(), "İŞDƏN_ÇIXARILDI", reason);
        return EmployeeResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "HR", entityType = "EMPLOYEE", isDelete = true)
    public void delete(Long id) {
        Employee e = loadActive(id);
        e.softDelete();
        employeeRepository.save(e);
        auditService.log("HR_İŞÇİ", e.getId(), e.getFullName(), "SİLİNDİ", "İşçi silindi");
    }

    private Employee loadActive(Long id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İşçi", id));
    }

    private String generateCode() {
        int year = LocalDate.now().getYear();
        String prefix = "EMP-" + year + "-";
        int next = employeeRepository.findMaxEmployeeCodeWithPrefix(prefix)
                .map(s -> {
                    try { return Integer.parseInt(s.substring(prefix.length())) + 1; }
                    catch (Exception ex) { return 1; }
                })
                .orElse(1);
        return prefix + String.format("%04d", next);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s; }
}
