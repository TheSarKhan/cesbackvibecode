package com.ces.erp.enums;

public enum AttendanceStatus implements LabeledEnum {
    PRESENT("İşdə"),
    ABSENT("Qayıb"),
    LEAVE("Məzuniyyətdə"),
    SICK("Xəstə"),
    HOLIDAY("Bayram"),
    BUSINESS_TRIP("Ezamiyyət");

    private final String label;
    AttendanceStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
