package com.xuan.erp.audit.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record AuditLogRecord(
        Long id,
        Long tenantId,
        String actorUsername,
        Long actorUserId,
        String action,
        String entityType,
        String entityId,
        String detail,
        String status,
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
        Boolean crossTenant,
        OffsetDateTime createdAt
) {
}
