package com.ces.erp.enums;

public enum RiskLevel implements LabeledEnum {
    LOW("Aşağı"),
    MEDIUM("Orta"),
    HIGH("Yüksək");

    private final String label;
    RiskLevel(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
