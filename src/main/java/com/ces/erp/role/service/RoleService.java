package com.ces.erp.role.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.permission.entity.Permission;
import com.ces.erp.permission.entity.Permission;
import com.ces.erp.permission.repository.PermissionRepository;
import com.ces.erp.role.dto.RoleRequest;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import com.ces.erp.role.dto.RoleResponse;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService implements ApprovalHandler {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
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

    /**
     * EDIT təsdiq diff-i üçün "sonrakı" snapshot — getSnapshot (RoleResponse) ilə eyni formada:
     * permissionIds → icazə code-ları, departmentId → şöbə adı; dəyişməyən sahə (active)
     * cari rolun dəyərini saxlayır. Beləcə diff hizalanır və xam rəqəm ID görünmür.
     */
    @Override
    public Object getAfterSnapshot(Long id, Object request) {
        if (!(request instanceof RoleRequest req)) return null;
        Role role = roleRepository.findByIdAndDeletedFalse(id).orElse(null);
        if (role == null) return null;

        String deptName = req.getDepartmentId() != null
                ? departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId()).map(Department::getName).orElse(null)
                : null;

        List<Permission> perms = (req.getPermissionIds() == null || req.getPermissionIds().isEmpty())
                ? List.of() : permissionRepository.findAllById(req.getPermissionIds());
        List<String> codes = perms.stream().map(Permission::getCode).sorted().toList();
        List<Long> permIds = perms.stream().map(Permission::getId).sorted().toList();

        List<RoleResponse.ApprovalDeptInfo> approvalDepts =
                (req.getApprovalDepartmentIds() == null || req.getApprovalDepartmentIds().isEmpty())
                ? List.of()
                : departmentRepository.findAllById(req.getApprovalDepartmentIds()).stream()
                        .map(d -> RoleResponse.ApprovalDeptInfo.builder().id(d.getId()).name(d.getName()).build())
                        .toList();

        return RoleResponse.builder()
                .id(role.getId())
                .name(req.getName())
                .description(req.getDescription())
                .departmentId(req.getDepartmentId())
                .departmentName(deptName)
                .active(role.isActive())
                .createdAt(role.getCreatedAt())
                .grantedPermissionIds(permIds)
                .permissions(codes)
                .approvalDepartments(approvalDepts)
                .build();
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

    public List<RoleResponse> getAll() {
        return roleRepository.findAllByDeletedFalse().stream()
                .map(RoleResponse::from)
                .toList();
    }

    public PagedResponse<RoleResponse> getAllPaged(int page, int size, String search, Long departmentId) {
        String q = (search != null && !search.isBlank()) ? search : null;
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PagedResponse.from(roleRepository.findAllFiltered(q, departmentId, pageable), RoleResponse::from);
    }

    public List<RoleResponse> getByDepartment(Long departmentId) {
        return roleRepository.findAllByDepartmentIdAndDeletedFalse(departmentId).stream()
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

        applyPermissions(role, request.getPermissionIds());
        saveApprovalDepartments(role, request.getApprovalDepartmentIds());
        role = roleRepository.save(role);
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

        applyPermissions(role, request.getPermissionIds());
        saveApprovalDepartments(role, request.getApprovalDepartmentIds());
        roleRepository.save(role);
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

    /** Verilmiş icazə ID-lərini rolun grantedPermissions dəstinə yazır (əvvəlkini əvəz edir). */
    private void applyPermissions(Role role, List<Long> permissionIds) {
        Set<Permission> granted = new LinkedHashSet<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            granted.addAll(permissionRepository.findAllById(permissionIds));
        }
        role.getGrantedPermissions().clear();
        role.getGrantedPermissions().addAll(granted);
    }

    private void saveApprovalDepartments(Role role, List<Long> approvalDepartmentIds) {
        role.getApprovalDepartments().clear();
        if (approvalDepartmentIds != null && !approvalDepartmentIds.isEmpty()) {
            List<Department> depts = departmentRepository.findAllById(approvalDepartmentIds);
            role.getApprovalDepartments().addAll(depts);
        }
    }
}
