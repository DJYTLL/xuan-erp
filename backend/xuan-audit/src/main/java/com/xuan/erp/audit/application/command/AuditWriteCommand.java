package com.xuan.erp.audit.application.command;

import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;

public record AuditWriteCommand(
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
        boolean crossTenant
) {

    public AuditWriteCommand {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
        }
        action = AuditActionNames.normalize(action);
        entityType = requireText(entityType, "审计实体类型不能为空");
        status = status == null ? AuditWriteStatus.SUCCESS : status;
        method = method == null ? null : method.trim().toUpperCase();
        path = trimToNull(path);
        requestId = trimToNull(requestId);
        clientIp = trimToNull(clientIp);
        userAgent = trimToNull(userAgent);
        errorCode = trimToNull(errorCode);
        errorMessage = trimToNull(errorMessage);
        authTenantCode = trimToNull(authTenantCode);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
