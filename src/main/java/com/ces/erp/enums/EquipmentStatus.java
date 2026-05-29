package com.ces.erp.enums;

public enum EquipmentStatus implements LabeledEnum {
    AVAILABLE("Mövcuddur"),
    RENTED("İcarədə"),
    IN_TRANSIT("Yoldadır"),
    IN_INSPECTION("Servisdədir"),
    UNDER_CHECK("Baxışda"),
    IN_REPAIR("Təmirdə"),
    DEFECTIVE("Nasaz"),
    OUT_OF_SERVICE("İstismardan çıxıb");

    private final String label;
    EquipmentStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
