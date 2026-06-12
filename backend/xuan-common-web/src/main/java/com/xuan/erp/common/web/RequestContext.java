package com.xuan.erp.common.web;

public final class RequestContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String traceId() {
        return TRACE_ID.get();
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long tenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
        TENANT_ID.remove();
    }
}
