package com.ces.erp.technicalservice.entity;

import com.ces.erp.enums.LabeledEnum;

public enum ServiceRecordType implements LabeledEnum {
    INSPECTION("Texniki baxış"),
    REPAIR("Təmir");

    private final String label;
    ServiceRecordType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
