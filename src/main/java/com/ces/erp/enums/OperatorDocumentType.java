package com.ces.erp.enums;

public enum OperatorDocumentType implements LabeledEnum {
    DRIVING_LICENSE("Sürücülük vəsiqəsi"),
    CRIMINAL_RECORD("Məhkumluq haqqında arayış"),
    HEALTH_CERTIFICATE("Sağlamlıq arayışı"),
    CERTIFICATE("Sertifikat"),
    ID_CARD("Şəxsiyyət vəsiqəsi"),
    POWER_OF_ATTORNEY("Etibarnamə");

    private final String label;
    OperatorDocumentType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
