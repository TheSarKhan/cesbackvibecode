package com.ces.erp.enums;

/**
 * Bütün etiketli enum-ların ortaq interfeysi.
 * <p>
 * Enum-un özü tək doğru mənbədir: Azərbaycan etiketi enum sabitinin yanında yaşayır
 * və {@code GET /api/enums} vasitəsilə frontend-ə tək nöqtədən paylaşılır.
 * Enum-lar DB-yə köçürülmür, sabit adları və workflow məntiqi dəyişmir — yalnız label əlavə olunur.
 */
public interface LabeledEnum {

    /** İstifadəçiyə göstərilən Azərbaycan dilli etiket. */
    String getLabel();

    /** Sabitin texniki kodu (default: enum {@code name()}). */
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}
