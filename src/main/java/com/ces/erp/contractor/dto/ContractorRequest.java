package com.ces.erp.contractor.dto;

import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractorRequest {

    @NotBlank(message = "Şirkət adı boş ola bilməz")
    private String companyName;

    @NotBlank(message = "VÖEN boş ola bilməz")
    @Pattern(regexp = "^\\d{10}$", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String voen;

    private String contactPerson;
    @Pattern(
            regexp = "^(\\+994|0)?[0-9]{9}$",
            message = "Düzgün telefon nömrəsi daxil edin"
    )
    private String phone;
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
