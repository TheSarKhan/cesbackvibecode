package com.ces.erp.enums;

public enum InvoiceStatus {
    DRAFT,     // Layihə meneceri tərəfindən yaradılan, mühasibatlığa göndərilməmiş
    SENT,      // Mühasibatlıq moduluna göndərilmiş
    APPROVED,  // Mühasibatlıq tərəfindən təsdiqlənmiş
    RETURNED   // Mühasibatlıq tərəfindən geri qaytarılmış
}
