package com.ces.erp.enums;

public enum ProjectStatus implements LabeledEnum {
    PENDING("Müqavilə gözlənilir"),    // Müqavilə gözlənilir
    ACTIVE("İcra mərhələsində"),     // Müqavilə yüklənib, icra mərhələsindədir
    COMPLETED("Bağlanmış");   // Bağlanmış, mühasibatlığa yönləndirilmişdir

    private final String label;
    ProjectStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
