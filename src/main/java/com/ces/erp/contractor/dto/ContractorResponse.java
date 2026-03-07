package com.ces.erp.contractor.dto;

import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ContractorResponse {

    private Long id;
    private String companyName;
    private String voen;
    private String contactPerson;
    private String phone;
    private String address;
    private String paymentType;
    private ContractorStatus status;
    private BigDecimal rating;
    private RiskLevel riskLevel;
    private String notes;
    private LocalDateTime createdAt;

    public static ContractorResponse from(Contractor c) {
        return ContractorResponse.builder()
                .id(c.getId())
                .companyName(c.getCompanyName())
                .voen(c.getVoen())
                .contactPerson(c.getContactPerson())
                .phone(c.getPhone())
                .address(c.getAddress())
                .paymentType(c.getPaymentType())
                .status(c.getStatus())
                .rating(c.getRating())
                .riskLevel(c.getRiskLevel())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
