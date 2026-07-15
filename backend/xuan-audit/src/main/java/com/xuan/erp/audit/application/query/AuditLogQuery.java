package com.xuan.erp.audit.application.query;

import com.xuan.erp.audit.application.command.AuditActionNames;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import java.time.OffsetDateTime;

public record AuditLogQuery(
        Long tenantId,
        String actorUsername,
        String action,
        String entityType,
        AuditWriteStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer limit
) {

    public AuditLogQuery {
        actorUsername = trimToNull(actorUsername);
        action = normalizeAction(action);
        entityType = trimToNull(entityType);
        limit = normalizeLimit(limit);
    }

    private static String normalizeAction(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AuditActionNames.normalize(value);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int normalizeLimit(Integer value) {
        if (value == null) {
            return 20;
        }
        return Math.min(100, Math.max(1, value));
    }
}
