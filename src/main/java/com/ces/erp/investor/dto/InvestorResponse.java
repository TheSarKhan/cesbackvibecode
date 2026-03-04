package com.ces.erp.investor.dto;

import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import com.ces.erp.investor.entity.Investor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvestorResponse {

    private Long id;
    private String companyName;
    private String voen;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String paymentType;
    private ContractorStatus status;
    private BigDecimal rating;
    private RiskLevel riskLevel;
    private String notes;
    private LocalDateTime createdAt;

    public static InvestorResponse from(Investor i) {
        return InvestorResponse.builder()
                .id(i.getId())
                .companyName(i.getCompanyName())
                .voen(i.getVoen())
                .contactPerson(i.getContactPerson())
                .contactPhone(i.getContactPhone())
                .address(i.getAddress())
                .paymentType(i.getPaymentType())
                .status(i.getStatus())
                .rating(i.getRating())
                .riskLevel(i.getRiskLevel())
                .notes(i.getNotes())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
