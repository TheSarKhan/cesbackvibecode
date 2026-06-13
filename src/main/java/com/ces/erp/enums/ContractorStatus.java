package com.ces.erp.enums;

public enum ContractorStatus implements LabeledEnum {
    ACTIVE("Aktiv"),
    INACTIVE("Deaktiv");

    private final String label;
    ContractorStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
