package com.ces.erp.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String department;
        private String role;
        private boolean hasApproval;
        private List<String> approvalDepartments;
        private List<ModulePermission> permissions;
    }

    @Data
    @Builder
    public static class ModulePermission {
        private String moduleCode;
        private String moduleNameAz;
        private boolean canGet;
        private boolean canPost;
        private boolean canPut;
        private boolean canDelete;
        private boolean canSendToCoordinator;
        private boolean canSubmitOffer;
    }
}
