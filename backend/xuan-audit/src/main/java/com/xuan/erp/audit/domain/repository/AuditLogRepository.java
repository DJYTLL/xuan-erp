package com.xuan.erp.audit.domain.repository;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.domain.model.AuditLog;
import java.util.List;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    default List<AuditLog> search(AuditLogQuery query) {
        return List.of();
    }
}
