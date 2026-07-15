package com.xuan.erp.audit.application.service;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogQueryApplicationService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogQueryApplicationService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> search(AuditLogQuery query) {
        return auditLogRepository.search(query);
    }
}
