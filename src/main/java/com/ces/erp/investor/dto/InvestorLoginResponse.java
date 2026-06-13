package com.ces.erp.investor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestorLoginResponse {

    private String accessToken;
    private String refreshToken;
    private InvestorInfo investor;

    @Data
    @Builder
    public static class InvestorInfo {
        private Long id;
        private String companyName;
        private String accountEmail;
        private String contactPerson;
        private String contactPhone;
    }
}
