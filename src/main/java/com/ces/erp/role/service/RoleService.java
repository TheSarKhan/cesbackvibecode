package com.ces.erp.role.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.role.dto.RoleRequest;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import com.ces.erp.role.dto.RoleResponse;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.entity.RolePermission;
import com.ces.erp.role.repository.RolePermissionRepository;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.systemmodule.entity.SystemModule;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService implements ApprovalHandler {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final DepartmentRepository departmentRepository;
    private final SystemModuleRepository systemModuleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "ROLE"; }
    @Override public String getModuleCode()  { return "ROLE_PERMISSION"; }
    @Override public String getLabel(Long id) {
        return roleRepository.findByIdAndDeletedFalse(id).map(Role::getName).orElse("Rol #" + id);
    }
    @Override public Object getSnapshot(Long id) {
        return RoleResponse.from(roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id)));
    }
    @Override
    public void applyEdit(Long id, String json) {
        try {
            RoleRequest req = objectMapper.readValue(json, RoleRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }
    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (!(auth.getPrincipal() instanceof UserPrincipal principal)) return false;
        return "Super Admin".equals(principal.getRoleName());
    }

    public List<RoleResponse> getAll() {
        boolean superAdmin = isSuperAdmin();
        return roleRepository.findAllByDeletedFalse().stream()
                .filter(r -> superAdmin || !"Super Admin".equals(r.getName()))
                .map(RoleResponse::from)
                .toList();
    }

    public PagedResponse<RoleResponse> getAllPaged(int page, int size, String search, Long departmentId) {
        String q = (search != null && !search.isBlank()) ? search : null;
        String excludeName = isSuperAdmin() ? null : "Super Admin";
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PagedResponse.from(roleRepository.findAllFiltered(q, departmentId, excludeName, pageable), RoleResponse::from);
    }

    public List<RoleResponse> getByDepartment(Long departmentId) {
        boolean superAdmin = isSuperAdmin();
        return roleRepository.findAllByDepartmentIdAndDeletedFalse(departmentId).stream()
                .filter(r -> superAdmin || !"Super Admin".equals(r.getName()))
                .map(RoleResponse::from)
                .toList();
    }

    public RoleResponse getById(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        Department dept = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", request.getDepartmentId()));

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .department(dept)
                .build();

        role = roleRepository.save(role);
        savePermissions(role, request);
        saveApprovalDepartments(role, request.getApprovalDepartmentIds());
        auditService.log("ROL", role.getId(), role.getName(), "YARADILDI", "Yeni rol yaradıldı");
        return RoleResponse.from(roleRepository.findByIdWithPermissions(role.getId()).orElseThrow());
    }

    @Transactional
    @RequiresApproval(module = "ROLE_PERMISSION", entityType = "ROLE")
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));

        Department dept = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", request.getDepartmentId()));

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setDepartment(dept);

        // İcazələri sıfırla və yenidən yaz
        rolePermissionRepository.deleteAllByRoleId(id);
        rolePermissionRepository.flush();
        roleRepository.save(role);
        savePermissions(role, request);
        saveApprovalDepartments(role, request.getApprovalDepartmentIds());
        auditService.log("ROL", role.getId(), role.getName(), "YENİLƏNDİ", "Rol məlumatları yeniləndi");
        return RoleResponse.from(roleRepository.findByIdWithPermissions(role.getId()).orElseThrow());
    }

    @Transactional
    @RequiresApproval(module = "ROLE_PERMISSION", entityType = "ROLE", isDelete = true)
    public void delete(Long id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));

        // Rola bağlı bütün userləri soft-delete et
        List<User> users = userRepository.findAllByRoleIdAndDeletedFalse(id);
        for (User user : users) {
            user.softDelete();
        }
        userRepository.saveAll(users);

        auditService.log("ROL", role.getId(), role.getName(), "SİLİNDİ",
                "Rol silindi. Bağlı " + users.size() + " istifadəçi deaktiv edildi");
        role.softDelete();
        roleRepository.save(role);
    }

    private void savePermissions(Role role, RoleRequest request) {
        if (request.getPermissions() == null || request.getPermissions().isEmpty()) return;

        List<RolePermission> permissions = request.getPermissions().stream()
                .map(pr -> {
                    SystemModule module = systemModuleRepository.findById(pr.getModuleId())
                            .orElseThrow(() -> new BusinessException("Modul tapılmadı. ID: " + pr.getModuleId()));
                    return RolePermission.builder()
                            .role(role)
                            .module(module)
                            .canGet(pr.isCanGet())
                            .canPost(pr.isCanPost())
                            .canPut(pr.isCanPut())
                            .canDelete(pr.isCanDelete())
                            .canSendToCoordinator(pr.isCanSendToCoordinator())
                            .canSubmitOffer(pr.isCanSubmitOffer())
                            .canSendToAccounting(pr.isCanSendToAccounting())
                            .canReturnToProject(pr.isCanReturnToProject())
                            .canApproveByPm(pr.isCanApproveByPm())
                            .canCheckDocuments(pr.isCanCheckDocuments())
                            .canDispatch(pr.isCanDispatch())
                            .canDeliver(pr.isCanDeliver())
                            .build();
                })
                .toList();

        rolePermissionRepository.saveAll(permissions);
    }

    private void saveApprovalDepartments(Role role, List<Long> approvalDepartmentIds) {
        role.getApprovalDepartments().clear();
        if (approvalDepartmentIds != null && !approvalDepartmentIds.isEmpty()) {
            List<Department> depts = departmentRepository.findAllById(approvalDepartmentIds);
            role.getApprovalDepartments().addAll(depts);
        }
        roleRepository.save(role);
    }
}
