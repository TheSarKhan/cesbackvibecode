package com.ces.erp.coordinator.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.operator.entity.Operator;
import com.ces.erp.projectmanager.entity.PartyType;
import com.ces.erp.projectmanager.entity.ShortlistItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Layihənin bir texnika xətti. Koordinator shortlist sətirlərindən bir neçəsini
 * seçir — hər seçim bir xətt olur və öz qiymətini, müddətini, operatorunu, sənəd
 * yoxlamasını, göndərmə/təhvil vəziyyətini və qaimələrini daşıyır.
 */
@Entity
@Table(name = "coordinator_plan_items", indexes = {
        @Index(name = "idx_cpi_plan", columnList = "plan_id"),
        @Index(name = "idx_cpi_deleted", columnList = "deleted"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinatorPlanItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private CoordinatorPlan plan;

    // Mənbə shortlist sətri (hansı namizəddən seçildi)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shortlist_item_id")
    private ShortlistItem shortlistItem;

    // Sahib (denormalizasiya — shortlist dəyişsə də sabit qalsın)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PartyType partyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    // ─── Qiymət (vahid başına: gün/ay) ───────────────────────────────────────
    // Sahibə ödəyəcəyimiz (cost). Şirkət texnikasında 0.
    @Column(name = "equipment_price", precision = 12, scale = 2)
    private BigDecimal equipmentPrice;

    // Müştəriyə təklif (revenue)
    @Column(name = "customer_equipment_price", precision = 12, scale = 2)
    private BigDecimal customerEquipmentPrice;

    // Daşınma (birdəfəlik) — bu texnika üçün
    @Column(name = "transportation_price", precision = 12, scale = 2)
    private BigDecimal transportationPrice;

    // ─── Sifarişçi ilə razılaşma (PM mərhələsi — hər xətt ayrıca) ─────────────
    // Koordinatorun təklifi (customerEquipmentPrice) başlanğıc nöqtədir; PM müştəri
    // ilə yekun qiyməti bu xətt üzrə ayrıca razılaşdırır.
    @Column(name = "agreed_equipment_price", precision = 12, scale = 2)
    private BigDecimal agreedEquipmentPrice;

    @Column(name = "agreed_transport_price", precision = 12, scale = 2)
    private BigDecimal agreedTransportPrice;

    @Column(name = "agreed_total_price", precision = 12, scale = 2)
    private BigDecimal agreedTotalPrice;

    @Column(name = "agreement_note", columnDefinition = "TEXT")
    private String agreementNote;

    // ─── Müddət (hər texnika öz dövrü) ────────────────────────────────────────
    private Integer dayCount;
    private LocalDate startDate;
    private LocalDate endDate;

    // ─── İcra (hər texnika ayrı) ──────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean equipmentDocsVerified = false;

    private LocalDateTime equipmentDocsCheckedAt;

    // Yoxlama checklist-ində işarələnmiş sənəd tiplərinin (config_item) id-ləri
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "coordinator_plan_item_checked_docs",
            joinColumns = @JoinColumn(name = "plan_item_id"))
    @Column(name = "config_item_id")
    @Builder.Default
    private Set<Long> checkedDocumentItemIds = new HashSet<>();

    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;

    @Column(columnDefinition = "TEXT")
    private String deliveryNotes;
}
