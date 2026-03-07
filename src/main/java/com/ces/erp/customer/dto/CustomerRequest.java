package com.ces.erp.customer.dto;

import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class CustomerRequest {

    @NotBlank(message = "Şirkət adı tələb olunur")
    private String companyName;

    private String voen;
    private String address;
    private String supplierPerson;
    private String supplierPhone;
    private String officeContactPerson;
    private String officeContactPhone;

    private Set<String> paymentTypes = new HashSet<>();

    private CustomerStatus status = CustomerStatus.ACTIVE;
    private RiskLevel riskLevel = RiskLevel.LOW;
    private String notes;
}
