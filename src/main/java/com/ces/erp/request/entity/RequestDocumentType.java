package com.ces.erp.request.entity;

import com.ces.erp.enums.LabeledEnum;

public enum RequestDocumentType implements LabeledEnum {
    CONTRACT("Müqavilə"),
    PRICE_PROTOCOL("Qiymət protokolu");

    private final String label;
    RequestDocumentType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
