package com.ces.erp.customer.dto;

import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class CustomerRequest {

    @NotBlank(message = "Şirkət adı tələb olunur")
    private String companyName;

    @Pattern(regexp = "^\\d{10}$", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String voen;
    private String address;
    private String supplierPerson;
    private String supplierPhone;
    private String officeContactPerson;
    @Pattern(
            regexp = "^(\\+994|0)?[0-9]{9}$",
            message = "Düzgün telefon nömrəsi daxil edin"
    )
    private String officeContactPhone;

    private Set<String> paymentTypes = new HashSet<>();

    private CustomerStatus status = CustomerStatus.ACTIVE;
    private RiskLevel riskLevel = RiskLevel.LOW;
    private String notes;
}
