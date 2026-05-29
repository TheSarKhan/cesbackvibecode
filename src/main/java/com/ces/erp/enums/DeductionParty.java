package com.ces.erp.enums;

/**
 * Tutulma aralığının aid olduğu tərəf.
 *   ISCI       — işçidən tutulur (net məbləğdən çıxıla bilər)
 *   ISEGOTUREN — işəgötürən töhfəsi (şirkət xərci, net-dən çıxılmır)
 */
public enum DeductionParty implements LabeledEnum {
    ISCI("İşçi"),
    ISEGOTUREN("İşəgötürən");

    private final String label;
    DeductionParty(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
