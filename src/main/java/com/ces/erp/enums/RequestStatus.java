package com.ces.erp.enums;

public enum RequestStatus implements LabeledEnum {
    DRAFT("Qaralama"),
    PENDING("Gözləyir"),
    PM_REVIEW("Layihə meneceri baxışı"),
    PM_SHORTLIST_READY("Qısa siyahı hazırdır"),
    COORDINATOR_NEGOTIATING("Koordinator danışıqlarda"),
    COORDINATOR_PROPOSED("Koordinator təklif verdi"),
    PM_PRICE_NEGOTIATION("Qiymət danışıqları"),
    PM_APPROVED("Layihə meneceri təsdiqlədi"),
    ACCOUNTING_DOCS_CHECK("Mühasibatlıq sənəd yoxlaması"),
    EXECUTION_READY("İcraya hazır"),
    OPERATOR_ASSIGNED("Operator təyin edildi"),
    EQUIPMENT_DISPATCHED("Texnika yola salındı"),
    DELIVERED("Təhvil verildi"),
    REJECTED("Rədd edildi");

    private final String label;
    RequestStatus(String label) { this.label = label; }
    @Override public String getLabel() { return label; }
}
