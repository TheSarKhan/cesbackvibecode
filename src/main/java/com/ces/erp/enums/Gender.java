package com.ces.erp.enums;

public enum Gender implements LabeledEnum {
    MALE("Kişi"),
    FEMALE("Qadın");

    private final String label;
    Gender(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
