package com.ces.erp.coordinator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CoordinatorPlanRequest {

    // ─── Mərhələ A — Danışıq ──────────────────────────────────────────────
    // Operator burda təyin EDİLMİR — yalnız icra fazasında (assignOperator).
    // Lakin köhnə klientlər üçün backward-compat olaraq saxlanılır.
    private Long operatorId;

    // Qalib shortlist sətri (yeni flow-da məcburi sahə)
    private Long winnerItemId;

    // Shortlist sətirlərinin danışıq qiyməti və rank-ı — koordinator
    // shortlist redaktoru kimi istifadə edir
    private List<ShortlistRowInput> shortlistRows;

    private Integer dayCount;
    // Podratçı/investora ödəyəcəyimiz texnika xərci (cost) — adətən winner.negotiatedPrice
    private BigDecimal equipmentPrice;
    // Sifarişçiyə təklif edilən texnika qiyməti (revenue) — koordinator təyin edir
    private BigDecimal customerEquipmentPrice;
    private BigDecimal contractorDailyRate;
    private BigDecimal contractorPayment;
    private BigDecimal operatorPayment;
    private BigDecimal transportationPrice;
    private Long transportContractorId;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<Long> safetyEquipmentIds;

    private String notes;

    // ─── Yeni model — layihəyə seçilmiş texnika xətləri (çoxlu) ────────────────
    // Bu siyahı verilirsə, koordinator çoxlu texnika seçmiş sayılır.
    private List<PlanItemInput> items;

    @Data
    public static class PlanItemInput {
        private Long id;                          // CoordinatorPlanItem id (update) / null (create)
        private Long shortlistItemId;             // mənbə shortlist sətri (create üçün məcburi)
        private BigDecimal equipmentPrice;        // sahibə ödəyəcəyimiz (cost) — şirkətdə 0
        private BigDecimal customerEquipmentPrice;// müştəriyə təklif (revenue)
        private BigDecimal transportationPrice;   // birdəfəlik daşınma (bu texnika üçün)
        private Integer dayCount;                 // öz müddəti
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class ShortlistRowInput {
        private Long itemId;             // null → yeni sətir yarat, var → mövcud sətri yenilə
        private String partyType;        // CONTRACTOR / INVESTOR (yeni sətirdə məcburi)
        private Long contractorId;
        private Long investorId;
        private Long equipmentId;
        private BigDecimal negotiatedPrice;
        private Integer rank;
        private String notes;
    }
}
