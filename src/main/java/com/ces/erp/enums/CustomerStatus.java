package com.ces.erp.enums;

public enum CustomerStatus implements LabeledEnum {
    ACTIVE("Aktiv"),
    PASSIVE("Passiv"),
    VARIABLE("Dəyişkən");

    private final String label;
    CustomerStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
