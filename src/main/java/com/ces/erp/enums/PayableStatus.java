package com.ces.erp.enums;

public enum PayableStatus implements LabeledEnum {
    PENDING("Ödəniş gözlənilir"),    // Ödəniş gözlənilir
    PARTIAL("Qismən ödənilib"),    // Qismən ödənilib
    OVERDUE("Gecikib"),    // Gecikib
    COMPLETED("Tam ödənilib");   // Tam ödənilib, yekunlaşıb

    private final String label;
    PayableStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
