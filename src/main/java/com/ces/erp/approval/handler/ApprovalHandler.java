package com.ces.erp.approval.handler;

public interface ApprovalHandler {

    String getEntityType();

    String getModuleCode();

    String getLabel(Long entityId);

    Object getSnapshot(Long entityId);

    /**
     * EDIT əməliyyatında "sonrakı" snapshot-u {@link #getSnapshot} ilə EYNİ formada qaytarır
     * (dəyişiklik tətbiq olunmadan). Belə olduqda təsdiq diff-i hizalanır (kod↔kod, ad↔ad).
     * Default {@code null} — bu halda aspect əvvəlki kimi request DTO-nu serialize edir.
     *
     * @param request annotasiyalı metodun ikinci arqumenti (adətən request DTO)
     */
    default Object getAfterSnapshot(Long entityId, Object request) {
        return null;
    }

    void applyEdit(Long entityId, String newSnapshotJson);

    void applyDelete(Long entityId);
}
