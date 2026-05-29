package com.ces.erp.enums;

/**
 * Tutulma növünün hansı tərəf(lər)ə aid olduğu.
 *   ISCI       — yalnız işçi tərəfi (məs. Gəlir vergisi)
 *   ISEGOTUREN — yalnız işəgötürən tərəfi
 *   HER_IKISI  — hər iki tərəf (məs. DSMF, İSH, İTS)
 *
 * Bir tutulma növünə yalnız applies_to ilə uyğun party-də aralıq əlavə edilə bilər
 * (validasiya {@code DeductionConfigService}-də).
 */
public enum DeductionAppliesTo implements LabeledEnum {
    ISCI("İşçi"),
    ISEGOTUREN("İşəgötürən"),
    HER_IKISI("Hər ikisi");

    private final String label;
    DeductionAppliesTo(String label) { this.label = label; }
    @Override public String getLabel() { return label; }

    /** Bu növ verilən party üçün aralıq qəbul edirmi? */
    public boolean allows(DeductionParty party) {
        return this == HER_IKISI
                || (this == ISCI && party == DeductionParty.ISCI)
                || (this == ISEGOTUREN && party == DeductionParty.ISEGOTUREN);
    }
}
