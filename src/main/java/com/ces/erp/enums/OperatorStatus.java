package com.ces.erp.enums;

public enum OperatorStatus implements LabeledEnum {
    EXCELLENT("Əla"),
    GOOD("Yaxşı"),
    NORMAL("Normal"),
    BAD("Pis");

    private final String label;
    OperatorStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
