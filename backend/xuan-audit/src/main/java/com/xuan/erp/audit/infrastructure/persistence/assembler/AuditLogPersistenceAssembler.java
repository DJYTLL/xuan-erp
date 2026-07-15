package com.xuan.erp.audit.infrastructure.persistence.assembler;

import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.audit.infrastructure.persistence.entity.AuditLogRecord;

public final class AuditLogPersistenceAssembler {

    private AuditLogPersistenceAssembler() {
    }

    public static AuditLogRecord toRecord(AuditLog auditLog) {
        return new AuditLogRecord(
                auditLog.id(),
                auditLog.tenantId(),
                auditLog.actorUsername(),
                auditLog.actorUserId(),
                auditLog.action(),
                auditLog.entityType(),
                auditLog.entityId(),
                auditLog.detail(),
                auditLog.status().code(),
                auditLog.requestId(),
                auditLog.clientIp(),
                auditLog.userAgent(),
                auditLog.durationMs(),
                auditLog.method(),
                auditLog.path(),
                auditLog.httpStatus(),
                auditLog.errorCode(),
                auditLog.errorMessage(),
                auditLog.authTenantId(),
                auditLog.authTenantCode(),
                auditLog.crossTenant(),
                auditLog.createdAt()
        );
    }

    public static AuditLog toDomain(AuditLogRecord record) {
        return new AuditLog(
                record.id(),
                record.tenantId(),
                record.actorUsername(),
                record.actorUserId(),
                record.action(),
                record.entityType(),
                record.entityId(),
                record.detail(),
                AuditWriteStatus.fromCode(record.status()),
                record.requestId(),
                record.clientIp(),
                record.userAgent(),
                record.durationMs(),
                record.method(),
                record.path(),
                record.httpStatus(),
                record.errorCode(),
                record.errorMessage(),
                record.authTenantId(),
                record.authTenantCode(),
                Boolean.TRUE.equals(record.crossTenant()),
                record.createdAt()
        );
    }
}
