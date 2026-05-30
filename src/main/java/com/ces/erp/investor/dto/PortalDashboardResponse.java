package com.ces.erp.investor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PortalDashboardResponse {

    // Avadanlıq
    private long equipmentCount;
    private Map<String, Long> equipmentByStatus; // status → say

    // Maliyyə (investora bizim borcumuz baxımından)
    private BigDecimal totalInvoiced;   // investora kəsilmiş qaimələrin cəmi
    private BigDecimal totalPaid;       // ödənilmiş cəm
    private BigDecimal outstanding;     // qalıq borc (totalAmount - paidAmount)
    private long openPayablesCount;     // tam ödənilməmiş ödəniş sayı
}
