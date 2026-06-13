package com.ces.erp.enums;

public enum OperationType implements LabeledEnum {
    EDIT("Redaktə"),
    DELETE("Silinmə");

    private final String label;
    OperationType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
