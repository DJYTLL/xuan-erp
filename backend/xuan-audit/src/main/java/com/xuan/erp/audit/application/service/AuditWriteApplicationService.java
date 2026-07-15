package com.xuan.erp.audit.application.service;

import com.xuan.erp.audit.application.command.AuditWriteCommand;
import com.xuan.erp.audit.application.result.AuditWriteResult;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditWriteApplicationService {

    private final AuditLogRepository auditLogRepository;

    public AuditWriteApplicationService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditWriteResult write(AuditWriteCommand command) {
        AuditLog saved = auditLogRepository.save(toAuditLog(command));
        return new AuditWriteResult(saved.id(), saved.status());
    }

    private AuditLog toAuditLog(AuditWriteCommand command) {
        return new AuditLog(
                null,
                command.tenantId(),
                command.actorUsername(),
                command.actorUserId(),
                command.action(),
                command.entityType(),
                command.entityId(),
                command.detail(),
                command.status(),
                command.requestId(),
                command.clientIp(),
                command.userAgent(),
                command.durationMs(),
                command.method(),
                command.path(),
                command.httpStatus(),
                command.errorCode(),
                command.errorMessage(),
                command.authTenantId(),
                command.authTenantCode(),
                command.crossTenant(),
                OffsetDateTime.now()
        );
    }
}
