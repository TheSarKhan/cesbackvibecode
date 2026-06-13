package com.ces.erp.investor.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Portal — bir texnikanın investora gətirdiyi qazanc.
 * Mənbə: həmin texnikaya (equipment_id ilə) bağlı INVESTOR_EXPENSE qaimələri +
 * texnikanın layihə tarixçəsi (utilization üçün).
 */
@Getter
@Builder
public class PortalEquipmentEarnings {

    private Long equipmentId;
    private BigDecimal totalEarn;       // bütün dövrlərin cəmi
    private BigDecimal monthEarn;       // cari ay
    private BigDecimal dailyRate;       // təxmini (son qaimənin günlük dərəcəsi)
    private Integer utilizationPct;     // son 12 ayda işləklik %, layihə tarixçəsindən
    private List<MonthPoint> trend;     // son 12 ay, köhnədən yeniyə

    @Getter
    @Builder
    public static class MonthPoint {
        private int year;
        private int month;              // 1..12
        private BigDecimal amount;
    }
}
