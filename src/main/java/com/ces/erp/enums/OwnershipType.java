package com.ces.erp.enums;

public enum OwnershipType implements LabeledEnum {
    COMPANY("Şirkət"),
    INVESTOR("İnvestor"),
    CONTRACTOR("Podratçı");

    private final String label;
    OwnershipType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
