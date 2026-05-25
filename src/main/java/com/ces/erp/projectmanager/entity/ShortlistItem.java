package com.ces.erp.projectmanager.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.investor.entity.Investor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shortlist_items", indexes = {
        @Index(name = "idx_shortlist_item_list", columnList = "shortlist_id"),
        @Index(name = "idx_shortlist_item_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shortlist_id", nullable = false)
    private RequestShortlist shortlist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
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

    // Koordinatorun bu sətr üçün danışdığı qiymət (Mərhələ A-da doldurulur)
    @Column(precision = 12, scale = 2)
    private BigDecimal negotiatedPrice;

    // Koordinatorun verdiyi prioritet sıralama (1 = ilk seçim, 2 = ikinci və s.)
    @Column(name = "priority_rank")
    private Integer rank;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
