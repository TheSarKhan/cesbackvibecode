package com.ces.erp.enums;

public enum EmployeeStatus implements LabeledEnum {
    ACTIVE("Aktiv"),
    ON_LEAVE("Məzuniyyətdə"),
    TERMINATED("İşdən çıxarılıb");

    private final String label;
    EmployeeStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
