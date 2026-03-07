package com.ces.erp.contractor.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "contractors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contractor extends BaseEntity {

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true, length = 20)
    private String voen;

    private String contactPerson;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractorStatus status;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
