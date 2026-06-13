package com.ces.erp.enums;

public enum InvoiceType implements LabeledEnum {
    INCOME("Gəlir"),              // A — Şirkət tərəfindən kəsilən (gəlir qaiməsi)
    CONTRACTOR_EXPENSE("Podratçı xərci"),  // B1 — Podratçıya kəsilən (xərc qaiməsi)
    COMPANY_EXPENSE("Şirkət xərci"),     // B2 — Şirkətə kəsilən (daxili xərc qaiməsi)
    INVESTOR_EXPENSE("İnvestor xərci");     // B3 — İnvestora kəsilən (investor ödəmə qaiməsi)

    private final String label;
    InvoiceType(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
