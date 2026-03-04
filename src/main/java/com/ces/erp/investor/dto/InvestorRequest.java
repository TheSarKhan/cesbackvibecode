package com.ces.erp.investor.dto;

import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvestorRequest {

    @NotBlank(message = "Şirkət adı boş ola bilməz")
    private String companyName;

    @NotBlank(message = "VÖEN boş ola bilməz")
    private String voen;

    private String contactPerson;
    private String contactPhone;
    private String address;
    private String paymentType;

    @NotNull(message = "Status boş ola bilməz")
    private ContractorStatus status;

    @DecimalMin(value = "0.00", message = "Reytinq 0-dan az ola bilməz")
    @DecimalMax(value = "5.00", message = "Reytinq 5-dən çox ola bilməz")
    private BigDecimal rating;

    @NotNull(message = "Risk səviyyəsi boş ola bilməz")
    private RiskLevel riskLevel;

    private String notes;
}
