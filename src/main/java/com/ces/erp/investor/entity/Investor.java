package com.ces.erp.investor.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "investors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investor extends BaseEntity {

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true, length = 20)
    private String voen;

    private String contactPerson;

    @Column(length = 50)
    private String contactPhone;

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

    // ─── Portal girişi (mobil investor tətbiqi) ──────────────────────────────
    // Investor şirkət-içi User DEYİL — ayrı kimlik doğrulaması ilə işləyir.

    @Column(unique = true)
    private String accountEmail;   // portal giriş maili (admin təyin edir, könüllü)

    private String passwordHash;   // BCrypt — heç bir API cavabında qaytarılmır

    @Column(nullable = false)
    @Builder.Default
    private boolean portalEnabled = false;

    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "investor", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<InvestorDocument> documents = new ArrayList<>();
}
