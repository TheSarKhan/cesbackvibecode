package com.ces.erp.enums;

public enum RecurrenceFrequency implements LabeledEnum {
    MONTHLY("Aylıq"),    // Aylıq
    QUARTERLY("Rüblük"),  // Rüblük
    ANNUAL("İllik");      // İllik

    private final String label;
    RecurrenceFrequency(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
