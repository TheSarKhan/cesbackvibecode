package com.ces.erp.enums;

public enum DocumentType implements LabeledEnum {
    HESAB_FAKTURA("Hesab-faktura"),
    TEHVIL_TESLIM_AKTI("Təhvil-təslim aktı"),
    ENGLISH_INVOICE("İngiliscə faktura");

    private final String label;
    DocumentType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
