package com.ces.erp.enums;

public enum AdjustmentType implements LabeledEnum {
    BONUS("Bonus"),
    PENALTY("Cərimə"),
    OVERTIME("Əlavə iş saatı"),
    VACATION_PAY("Məzuniyyət haqqı"),
    OTHER("Digər");

    private final String label;
    AdjustmentType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
