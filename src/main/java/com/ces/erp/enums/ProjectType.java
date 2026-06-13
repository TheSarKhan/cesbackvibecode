package com.ces.erp.enums;

public enum ProjectType implements LabeledEnum {
    DAILY("Günlük"),
    MONTHLY("Aylıq");

    private final String label;
    ProjectType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
