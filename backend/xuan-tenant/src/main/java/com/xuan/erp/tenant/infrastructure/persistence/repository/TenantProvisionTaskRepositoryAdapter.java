package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantProvisionTaskRecord;
import com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantProvisionTaskPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantProvisionTaskRepositoryAdapter implements TenantProvisionTaskRepository {

    private final TenantProvisionTaskPersistenceMapper mapper;

    public TenantProvisionTaskRepositoryAdapter(TenantProvisionTaskPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<TenantProvisionTask> findById(Long taskId) {
        return Optional.ofNullable(mapper.findById(taskId)).map(this::toDomain);
    }

    @Override
    public List<TenantProvisionTask> findActiveByTenantId(Long tenantId) {
        return mapper.findActiveByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<TenantProvisionTask> findActiveByTenantIdAndTaskKey(Long tenantId, String taskKey) {
        return Optional.ofNullable(mapper.findActiveByTenantIdAndTaskKey(tenantId, taskKey)).map(this::toDomain);
    }

    @Override
    public Optional<TenantProvisionTask> findActiveByTaskKeyAndIdempotencyKey(String taskKey, String idempotencyKey) {
        return Optional.ofNullable(mapper.findActiveByTaskKeyAndIdempotencyKey(taskKey, idempotencyKey)).map(this::toDomain);
    }

    @Override
    public TenantProvisionTask save(TenantProvisionTask task) {
        TenantProvisionTaskRecord record = toRecord(task);
        if (task.id() == null) {
            mapper.insert(record);
            return Optional.ofNullable(mapper.findActiveByTaskKeyAndIdempotencyKey(task.taskKey(), task.idempotencyKey()))
                    .map(this::toDomain)
                    .orElse(task);
        }
        mapper.update(record);
        return Optional.ofNullable(mapper.findByIdIncludingDeleted(task.id()))
                .map(this::toDomain)
                .orElse(task);
    }

    private TenantProvisionTask toDomain(TenantProvisionTaskRecord record) {
        return new TenantProvisionTask(
                record.id(),
                record.tenantId(),
                record.taskKey(),
                record.taskType(),
                ProvisionTaskStatus.fromCode(record.status()),
                record.idempotencyKey(),
                record.stepName(),
                record.requestPayloadJson(),
                record.resultPayloadJson(),
                record.retryCount(),
                record.maxRetryCount(),
                record.lastErrorCode(),
                record.lastErrorMessage(),
                record.startedAt(),
                record.finishedAt(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt(),
                record.deletedBy(),
                record.deleteReason(),
                record.deletedAt()
        );
    }

    private TenantProvisionTaskRecord toRecord(TenantProvisionTask task) {
        return new TenantProvisionTaskRecord(
                task.id(),
                task.tenantId(),
                task.taskKey(),
                task.taskType(),
                task.status().code(),
                task.idempotencyKey(),
                task.stepName(),
                task.requestPayloadJson(),
                task.resultPayloadJson(),
                task.retryCount(),
                task.maxRetryCount(),
                task.lastErrorCode(),
                task.lastErrorMessage(),
                task.startedAt(),
                task.finishedAt(),
                task.createdBy(),
                task.createdAt(),
                task.updatedBy(),
                task.updatedAt(),
                task.deletedBy(),
                task.deleteReason(),
                task.deletedAt()
        );
    }
}
