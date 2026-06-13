package com.ces.erp.role.dto;

import com.ces.erp.role.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private Long departmentId;
    private String departmentName;
    private boolean active;
    private LocalDateTime createdAt;
    private List<Long> grantedPermissionIds;   // rol redaktə ekranında checkbox-ları doldurmaq üçün
    private List<String> permissions;           // verilmiş icazə code-ları (göstərim/diff üçün)
    private List<ApprovalDeptInfo> approvalDepartments;

    @Data
    @Builder
    public static class ApprovalDeptInfo {
        private Long id;
        private String name;
    }

    public static RoleResponse from(Role role) {
        List<Long> grantedIds = role.getGrantedPermissions() == null ? List.of() :
                role.getGrantedPermissions().stream().map(p -> p.getId()).sorted().toList();
        List<String> codes = role.getGrantedPermissions() == null ? List.of() :
                role.getGrantedPermissions().stream().map(p -> p.getCode()).sorted().toList();

        List<ApprovalDeptInfo> approvalDepts = role.getApprovalDepartments() == null ? List.of() :
                role.getApprovalDepartments().stream()
                        .map(d -> ApprovalDeptInfo.builder().id(d.getId()).name(d.getName()).build())
                        .toList();

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .departmentId(role.getDepartment() != null ? role.getDepartment().getId() : null)
                .departmentName(role.getDepartment() != null ? role.getDepartment().getName() : null)
                .active(role.isActive())
                .createdAt(role.getCreatedAt())
                .grantedPermissionIds(grantedIds)
                .permissions(codes)
                .approvalDepartments(approvalDepts)
                .build();
    }
}
