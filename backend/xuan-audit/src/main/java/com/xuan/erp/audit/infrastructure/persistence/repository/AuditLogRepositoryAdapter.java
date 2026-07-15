package com.xuan.erp.audit.infrastructure.persistence.repository;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.repository.AuditLogRepository;
import com.xuan.erp.audit.infrastructure.persistence.assembler.AuditLogPersistenceAssembler;
import com.xuan.erp.audit.infrastructure.persistence.entity.AuditLogRecord;
import com.xuan.erp.audit.infrastructure.persistence.mapper.AuditLogPersistenceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogPersistenceMapper mapper;

    public AuditLogRepositoryAdapter(AuditLogPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogRecord record = AuditLogPersistenceAssembler.toRecord(auditLog);
        Long id = mapper.insert(record);
        return AuditLogPersistenceAssembler.toDomain(record).withId(id);
    }

    @Override
    public List<AuditLog> search(AuditLogQuery query) {
        return mapper.search(query).stream()
                .map(AuditLogPersistenceAssembler::toDomain)
                .toList();
    }
}
