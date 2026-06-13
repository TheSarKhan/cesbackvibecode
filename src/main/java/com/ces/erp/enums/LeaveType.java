package com.ces.erp.enums;

public enum LeaveType implements LabeledEnum {
    ANNUAL("İllik məzuniyyət"),
    SICK("Xəstəlik məzuniyyəti"),
    UNPAID("Ödənişsiz məzuniyyət"),
    MATERNITY("Analıq məzuniyyəti"),
    BUSINESS_TRIP("Ezamiyyət");

    private final String label;
    LeaveType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
