package com.ces.erp.department.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ces.erp.department.dto.DepartmentRequest;
import com.ces.erp.department.dto.DepartmentResponse;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService implements ApprovalHandler {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "DEPARTMENT"; }
    @Override public String getModuleCode()  { return "ROLE_PERMISSION"; }
    @Override public String getLabel(Long id) {
        return departmentRepository.findByIdAndDeletedFalse(id).map(Department::getName).orElse("Şöbə #" + id);
    }
    @Override public Object getSnapshot(Long id) {
        return DepartmentResponse.from(departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id)));
    }
    @Override
    public void applyEdit(Long id, String json) {
        try {
            DepartmentRequest req = objectMapper.readValue(json, DepartmentRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }
    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

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
            throw new DuplicateResourceException("Bu adda şöbə artıq mövcuddur");
        }
        Department dept = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Department saved = departmentRepository.save(dept);
        auditService.log("ŞÖBƏ", saved.getId(), saved.getName(), "YARADILDI", "Yeni şöbə yaradıldı");
        return DepartmentResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "ROLE_PERMISSION", entityType = "DEPARTMENT")
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department dept = departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id));
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        Department saved = departmentRepository.save(dept);
        auditService.log("ŞÖBƏ", saved.getId(), saved.getName(), "YENİLƏNDİ", "Şöbə məlumatları yeniləndi");
        return DepartmentResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "ROLE_PERMISSION", entityType = "DEPARTMENT", isDelete = true)
    public void delete(Long id) {
        Department dept = departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id));

        // Şöbəyə bağlı bütün rolları soft-delete et
        List<Role> roles = roleRepository.findAllByDepartmentIdAndDeletedFalse(id);
        for (Role role : roles) {
            // Rola bağlı bütün userləri soft-delete et
            List<User> roleUsers = userRepository.findAllByRoleIdAndDeletedFalse(role.getId());
            for (User user : roleUsers) {
                user.softDelete();
            }
            userRepository.saveAll(roleUsers);
            role.softDelete();
        }
        roleRepository.saveAll(roles);

        // Şöbəyə birbaşa bağlı (rolu olmayan) userləri soft-delete et
        List<User> deptUsers = userRepository.findAllByDepartmentIdAndDeletedFalse(id);
        for (User user : deptUsers) {
            user.softDelete();
        }
        userRepository.saveAll(deptUsers);

        auditService.log("ŞÖBƏ", dept.getId(), dept.getName(), "SİLİNDİ",
                "Şöbə silindi. Bağlı " + roles.size() + " rol və " + deptUsers.size() + " istifadəçi deaktiv edildi");
        dept.softDelete();
        departmentRepository.save(dept);
    }
}
