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
        private String role;              // convenience — ilk rolun adı (display)
        private List<String> roleNames;   // bütün rolların adları
        private boolean hasApproval;
        private List<String> approvalDepartments;
        private List<String> permissions; // effektiv icazə code-ları (MODULE:ACTION)
    }
}
