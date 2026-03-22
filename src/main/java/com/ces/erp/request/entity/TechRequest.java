package com.ces.erp.request.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.ProjectType;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tech_requests", indexes = {
        @Index(name = "idx_techrequest_status", columnList = "status"),
        @Index(name = "idx_techrequest_deleted", columnList = "deleted"),
        @Index(name = "idx_techrequest_customer", columnList = "customer_id"),
        @Index(name = "idx_techrequest_region", columnList = "region"),
        @Index(name = "idx_techrequest_created_by", columnList = "created_by_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechRequest extends BaseEntity {

    // ─── Əsas məlumatlar ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.DRAFT;

    @Column(length = 20, unique = true)
    private String requestCode;

    // ─── Müştəri məlumatları ──────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false)
    private String companyName;

    private String contactPerson;

    @Column(length = 50)
    private String contactPhone;

    // ─── Layihə məlumatları ───────────────────────────────────────────────────

    private String projectName;

    private String region;

    private LocalDate requestDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProjectType projectType;

    private Integer dayCount;

    // ─── Daşınma ──────────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean transportationRequired = false;

    // ─── Texniki parametrlər (Key-Value) ──────────────────────────────────────

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tech_request_params", joinColumns = @JoinColumn(name = "request_id"))
    @Builder.Default
    private List<TechParam> params = new ArrayList<>();

    // ─── Seçilmiş texnika ─────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment selectedEquipment;

    // ─── Yaradan istifadəçi ───────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
