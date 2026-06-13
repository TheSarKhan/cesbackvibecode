package com.ces.erp.user.dto;

import com.ces.erp.role.entity.Role;
import com.ces.erp.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Long departmentId;
    private String departmentName;
    // Çoxlu rol
    private List<Long> roleIds;
    private List<String> roleNames;
    // Convenience (geriyə uyğunluq / display) — ilk rol
    private Long roleId;
    private String roleName;
    private boolean active;
    private boolean hasApproval;
    private List<ApprovalDeptInfo> approvalDepartments;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    // Effektiv icazə code-ları (bütün rolların birləşməsi); super admin üçün boş — frontend flag-a baxır
    private List<String> permissions;

    @Data
    @Builder
    public static class ApprovalDeptInfo {
        private Long id;
        private String name;
    }

    public static UserResponse from(User user) {
        List<ApprovalDeptInfo> approvalDepts = user.getApprovalDepartments() == null ? List.of() :
                user.getApprovalDepartments().stream()
                        .map(ad -> ApprovalDeptInfo.builder()
                                .id(ad.getDepartment().getId())
                                .name(ad.getDepartment().getName())
                                .build())
                        .toList();

        List<Role> roles = user.getRoles() == null ? List.of() : user.getRoles().stream().toList();

        Set<String> codes = new LinkedHashSet<>();
        roles.forEach(r -> {
            if (r.getGrantedPermissions() != null)
                r.getGrantedPermissions().forEach(p -> codes.add(p.getCode()));
        });

        Role primary = roles.stream().findFirst().orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .roleIds(roles.stream().map(Role::getId).toList())
                .roleNames(roles.stream().map(Role::getName).toList())
                .roleId(primary != null ? primary.getId() : null)
                .roleName(primary != null ? primary.getName() : null)
                .active(user.isActive())
                .hasApproval(user.isHasApproval())
                .approvalDepartments(approvalDepts)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .permissions(codes.stream().sorted().toList())
                .build();
    }
}
