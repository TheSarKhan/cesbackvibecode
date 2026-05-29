package com.ces.erp.enums;

public enum ReceivableStatus implements LabeledEnum {
    PENDING("Ödəniş gözlənilir"),    // Ödəniş gözlənilir (layihə bitib, müddət var)
    PARTIAL("Qismən ödənilib"),    // Qismən ödənilib (hissə-hissə)
    OVERDUE("Gecikib"),    // Gecikib (20 gün keçib, tam ödənilməyib)
    COMPLETED("Tam ödənilib");   // Tam ödənilib, yekunlaşıb

    private final String label;
    ReceivableStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
