package com.ces.erp.enums;

public enum InvoiceStatus implements LabeledEnum {
    DRAFT("Qaralama"),     // Layihə meneceri tərəfindən yaradılan, mühasibatlığa göndərilməmiş
    SENT("Göndərilib"),      // Mühasibatlıq moduluna göndərilmiş
    APPROVED("Təsdiqlənib"),  // Mühasibatlıq tərəfindən təsdiqlənmiş
    RETURNED("Geri qaytarılıb");   // Mühasibatlıq tərəfindən geri qaytarılmış

    private final String label;
    InvoiceStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
