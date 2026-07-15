package com.xuan.erp.audit.interfaces.dto;

import com.xuan.erp.audit.application.command.AuditWriteCommand;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;

public record AuditWriteRequest(
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
        Boolean crossTenant
) {

    public AuditWriteCommand toCommand() {
        return new AuditWriteCommand(
                tenantId,
                actorUsername,
                actorUserId,
                action,
                entityType,
                entityId,
                detail,
                AuditWriteStatus.fromCode(status),
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
                Boolean.TRUE.equals(crossTenant)
        );
    }
}
