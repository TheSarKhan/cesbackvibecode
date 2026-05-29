package com.ces.erp.projectmanager.entity;

import com.ces.erp.enums.LabeledEnum;

public enum PartyType implements LabeledEnum {
    COMPANY("Şirkət"),
    CONTRACTOR("Podratçı"),
    INVESTOR("İnvestor");

    private final String label;
    PartyType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
