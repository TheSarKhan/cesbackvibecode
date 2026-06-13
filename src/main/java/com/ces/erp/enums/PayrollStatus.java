package com.ces.erp.enums;

public enum PayrollStatus implements LabeledEnum {
    DRAFT("Qaralama"),
    APPROVED("Təsdiqləndi"),
    PAID("Ödənilib");

    private final String label;
    PayrollStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
