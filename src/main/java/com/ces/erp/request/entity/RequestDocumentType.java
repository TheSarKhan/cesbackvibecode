package com.ces.erp.request.entity;

import com.ces.erp.enums.LabeledEnum;

public enum RequestDocumentType implements LabeledEnum {
    // Müştəri tərəfi (biz → müştəri)
    CONTRACT("Müqavilə"),
    PRICE_PROTOCOL("Qiymət protokolu"),
    // Sahib tərəfi (podratçı/investor → biz) — yalnız texnika bizimki olmayanda
    OWNER_CONTRACT("Sahib müqaviləsi"),
    OWNER_PRICE_PROTOCOL("Sahib qiymət protokolu");

    private final String label;
    RequestDocumentType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
