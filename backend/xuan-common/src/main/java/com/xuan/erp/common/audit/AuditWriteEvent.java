package com.xuan.erp.common.audit;

public record AuditWriteEvent(
        Long tenantId,
        String actorUsername,
        Long actorUserId,
        String action,
        String entityType,
        String entityId,
        String detail,
        AuditWriteOutcome status,
        String requestId,
        String clientIp,
        String userAgent,
        Long durationMs,
        String method,
        String path,
        Integer httpStatus,
        String errorCode,
        String errorMessage,
        Long authTenantId,
        String authTenantCode,
        boolean crossTenant
) {
}
