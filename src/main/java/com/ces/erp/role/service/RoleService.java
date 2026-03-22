package com.ces.erp.role.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.role.dto.RoleRequest;
import com.ces.erp.role.dto.RoleResponse;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.entity.RolePermission;
import com.ces.erp.role.repository.RolePermissionRepository;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.systemmodule.entity.SystemModule;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final DepartmentRepository departmentRepository;
    private final SystemModuleRepository systemModuleRepository;

    public List<RoleResponse> getAll() {
        return roleRepository.findAllByDeletedFalse().stream()
                .map(RoleResponse::from)
                .toList();
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

        role = roleRepository.save(role);
        savePermissions(role, request);
        saveApprovalDepartments(role, request.getApprovalDepartmentIds());
        return RoleResponse.from(roleRepository.findByIdWithPermissions(role.getId()).orElseThrow());
    }

    @Transactional
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

        return RoleResponse.from(roleRepository.findByIdWithPermissions(role.getId()).orElseThrow());
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
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
