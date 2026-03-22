package com.ces.erp.role.dto;

import com.ces.erp.role.entity.Role;
import com.ces.erp.role.entity.RolePermission;
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
    private List<PermissionResponse> permissions;
    private List<ApprovalDeptInfo> approvalDepartments;

    @Data
    @Builder
    public static class ApprovalDeptInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class PermissionResponse {
        private Long moduleId;
        private String moduleCode;
        private String moduleNameAz;
        private boolean canGet;
        private boolean canPost;
        private boolean canPut;
        private boolean canDelete;
        private boolean canSendToCoordinator;
        private boolean canSubmitOffer;

        public static PermissionResponse from(RolePermission p) {
            return PermissionResponse.builder()
                    .moduleId(p.getModule().getId())
                    .moduleCode(p.getModule().getCode())
                    .moduleNameAz(p.getModule().getNameAz())
                    .canGet(p.isCanGet())
                    .canPost(p.isCanPost())
                    .canPut(p.isCanPut())
                    .canDelete(p.isCanDelete())
                    .canSendToCoordinator(p.isCanSendToCoordinator())
                    .canSubmitOffer(p.isCanSubmitOffer())
                    .build();
        }
    }

    public static RoleResponse from(Role role) {
        List<PermissionResponse> perms = role.getPermissions() == null ? List.of() :
                role.getPermissions().stream().map(PermissionResponse::from).toList();

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
                .permissions(perms)
                .approvalDepartments(approvalDepts)
                .build();
    }
}
