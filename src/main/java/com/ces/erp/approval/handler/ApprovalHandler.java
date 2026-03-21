package com.ces.erp.approval.handler;

public interface ApprovalHandler {

    String getEntityType();

    String getModuleCode();

    String getLabel(Long entityId);

    Object getSnapshot(Long entityId);

    void applyEdit(Long entityId, String newSnapshotJson);

    void applyDelete(Long entityId);
}
