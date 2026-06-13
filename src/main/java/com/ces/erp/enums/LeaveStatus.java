package com.ces.erp.enums;

public enum LeaveStatus implements LabeledEnum {
    PENDING("Gözləyir"),
    APPROVED("Təsdiqləndi"),
    REJECTED("Rədd edildi"),
    CANCELLED("Ləğv edildi");

    private final String label;
    LeaveStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
