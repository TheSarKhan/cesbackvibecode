package com.ces.erp.enums;

public enum OperationStatus implements LabeledEnum {
    PENDING("Gözləyir"),
    APPROVED("Təsdiqləndi"),
    REJECTED("Rədd edildi");

    private final String label;
    OperationStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
