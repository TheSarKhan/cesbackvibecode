package com.ces.erp.enums;

public enum ReceivableStatus {
    PENDING,    // Ödəniş gözlənilir (layihə bitib, müddət var)
    PARTIAL,    // Qismən ödənilib (hissə-hissə)
    OVERDUE,    // Gecikib (20 gün keçib, tam ödənilməyib)
    COMPLETED   // Tam ödənilib, yekunlaşıb
}
