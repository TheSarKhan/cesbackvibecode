package com.ces.erp.customer.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String companyName;

    @Column(length = 20)
    private String voen;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String supplierPerson;

    @Column(length = 50)
    private String supplierPhone;

    private String officeContactPerson;

    @Column(length = 50)
    private String officeContactPhone;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_payment_types", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "payment_type")
    @Builder.Default
    private Set<String> paymentTypes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerDocument> documents = new ArrayList<>();
}
