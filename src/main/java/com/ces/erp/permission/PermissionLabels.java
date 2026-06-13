package com.ces.erp.permission;

import java.util.Map;

/**
 * İcazə action-ları üçün Azərbaycan etiketləri + humanize köməkçisi.
 * Həm {@code PermissionScanner}, həm seeder tərəfindən istifadə olunur.
 */
public final class PermissionLabels {

    private PermissionLabels() {}

    private static final Map<String, String> ACTION_LABELS = Map.ofEntries(
            Map.entry("GET", "Oxumaq"),
            Map.entry("POST", "Yazmaq"),
            Map.entry("PUT", "Redaktə"),
            Map.entry("DELETE", "Silmək"),
            Map.entry("SEND_COORDINATOR", "Koordinatora göndər"),
            Map.entry("SUBMIT_OFFER", "Təklif göndər"),
            Map.entry("SEND_ACCOUNTING", "Mühasibatlığa göndər"),
            Map.entry("RETURN_PROJECT", "Layihəyə geri göndər"),
            Map.entry("APPROVE_PM", "PM təsdiqi"),
            Map.entry("CHECK_DOCUMENTS", "Sənəd təsdiqi"),
            Map.entry("DISPATCH", "Texnika göndər"),
            Map.entry("DELIVER", "Təhvil-təslim")
    );

    /** Action üçün AZ etiket; məlum deyilsə humanize (UPPER_SNAKE → "Söz söz"). */
    public static String actionLabel(String action) {
        String known = ACTION_LABELS.get(action);
        if (known != null) return known;
        return humanize(action);
    }

    /** Tam etiket: "{Modul adı} — {Action etiketi}". */
    public static String fullLabel(String moduleNameAz, String action) {
        String prefix = (moduleNameAz != null && !moduleNameAz.isBlank()) ? moduleNameAz + " — " : "";
        return prefix + actionLabel(action);
    }

    /** UPPER_SNAKE / camelCase → oxunaqlı (ilk hərf böyük, qalan kiçik). */
    public static String humanize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replaceAll("([a-z\\d])([A-Z])", "$1 $2")
                .replaceAll("[_-]+", " ")
                .trim()
                .toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
