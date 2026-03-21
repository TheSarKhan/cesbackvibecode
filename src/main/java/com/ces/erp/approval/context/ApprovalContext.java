package com.ces.erp.approval.context;

public class ApprovalContext {

    private static final ThreadLocal<Boolean> applying = ThreadLocal.withInitial(() -> false);

    public static void setApplying(boolean value) {
        applying.set(value);
    }

    public static boolean isApplying() {
        return Boolean.TRUE.equals(applying.get());
    }

    public static void clear() {
        applying.remove();
    }
}
