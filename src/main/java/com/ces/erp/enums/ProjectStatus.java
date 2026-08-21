package com.ces.erp.enums;

public enum ProjectStatus implements LabeledEnum {
    PENDING("Müqavilə gözlənilir"),    // Müqavilə gözlənilir
    ACTIVE("İcra mərhələsində"),     // Müqavilə yüklənib, icra mərhələsindədir
    PAUSED("Dayandırılıb"),           // Müvəqqəti dayandırılıb / Dondurulub
    COMPLETED("Bağlanmış"),          // Bağlanmış, mühasibatlığa yönləndirilmişdir
    CANCELLED("Ləğv edilmiş");        // Vaxtından əvvəl xitam verilib / Ləğv edilib

    private final String label;
    ProjectStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
