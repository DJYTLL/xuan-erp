package com.xuan.erp.audit.domain.model;

import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import java.time.OffsetDateTime;

public record AuditLog(
        Long id,
        Long tenantId,
        String actorUsername,
        Long actorUserId,
        String action,
        String entityType,
        String entityId,
        String detail,
        AuditWriteStatus status,
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
        boolean crossTenant,
        OffsetDateTime createdAt
) {

    public AuditLog withId(Long id) {
        return new AuditLog(
                id,
                tenantId,
                actorUsername,
                actorUserId,
                action,
                entityType,
                entityId,
                detail,
                status,
                requestId,
                clientIp,
                userAgent,
                durationMs,
                method,
                path,
                httpStatus,
                errorCode,
                errorMessage,
                authTenantId,
                authTenantCode,
                crossTenant,
                createdAt
        );
    }
}
