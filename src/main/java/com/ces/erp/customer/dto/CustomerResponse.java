package com.ces.erp.customer.dto;

import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class CustomerResponse {

    private Long id;
    private String companyName;
    private String voen;
    private String address;
    private String supplierPerson;
    private String supplierPhone;
    private String officeContactPerson;
    private String officeContactPhone;
    private Set<String> paymentTypes;
    private CustomerStatus status;
    private RiskLevel riskLevel;
    private String notes;
    private List<CustomerDocumentResponse> documents;
    private LocalDateTime createdAt;

    public static CustomerResponse from(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .companyName(c.getCompanyName())
                .voen(c.getVoen())
                .address(c.getAddress())
                .supplierPerson(c.getSupplierPerson())
                .supplierPhone(c.getSupplierPhone())
                .officeContactPerson(c.getOfficeContactPerson())
                .officeContactPhone(c.getOfficeContactPhone())
                .paymentTypes(c.getPaymentTypes())
                .status(c.getStatus())
                .riskLevel(c.getRiskLevel())
                .notes(c.getNotes())
                .documents(c.getDocuments().stream()
                        .filter(d -> !d.isDeleted())
                        .map(CustomerDocumentResponse::from)
                        .toList())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
