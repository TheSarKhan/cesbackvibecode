package com.ces.erp.user.dto;

import com.ces.erp.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Long departmentId;
    private String departmentName;
    private Long roleId;
    private String roleName;
    private boolean active;
    private boolean hasApproval;
    private List<ApprovalDeptInfo> approvalDepartments;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private List<PermissionInfo> permissions;

    @Data
    @Builder
    public static class ApprovalDeptInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class PermissionInfo {
        private String moduleCode;
        private String moduleName;
        private boolean canGet;
        private boolean canPost;
        private boolean canPut;
        private boolean canDelete;
    }

    public static UserResponse from(User user) {
        List<ApprovalDeptInfo> approvalDepts = user.getApprovalDepartments() == null ? List.of() :
                user.getApprovalDepartments().stream()
                        .map(ad -> ApprovalDeptInfo.builder()
                                .id(ad.getDepartment().getId())
                                .name(ad.getDepartment().getName())
                                .build())
                        .toList();

        List<PermissionInfo> permissions = List.of();
        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            permissions = user.getRole().getPermissions().stream()
                    .map(p -> PermissionInfo.builder()
                            .moduleCode(p.getModule().getCode())
                            .moduleName(p.getModule().getNameAz())
                            .canGet(p.isCanGet())
                            .canPost(p.isCanPost())
                            .canPut(p.isCanPut())
                            .canDelete(p.isCanDelete())
                            .build())
                    .toList();
        }

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .active(user.isActive())
                .hasApproval(user.isHasApproval())
                .approvalDepartments(approvalDepts)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .permissions(permissions)
                .build();
    }
}
