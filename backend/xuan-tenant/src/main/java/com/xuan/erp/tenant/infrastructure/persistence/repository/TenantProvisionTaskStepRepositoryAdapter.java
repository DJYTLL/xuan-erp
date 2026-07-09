package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskStepRepository;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantProvisionTaskStepRecord;
import com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantProvisionTaskStepPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantProvisionTaskStepRepositoryAdapter implements TenantProvisionTaskStepRepository {

    private final TenantProvisionTaskStepPersistenceMapper mapper;

    public TenantProvisionTaskStepRepositoryAdapter(TenantProvisionTaskStepPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<TenantProvisionTaskStep> findByTaskId(Long taskId) {
        return mapper.findByTaskId(taskId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<TenantProvisionTaskStep> findActiveByTaskIdAndStepKey(Long taskId, String stepKey) {
        return Optional.ofNullable(mapper.findActiveByTaskIdAndStepKey(taskId, stepKey))
                .map(this::toDomain);
    }

    @Override
    public TenantProvisionTaskStep save(TenantProvisionTaskStep step) {
        TenantProvisionTaskStepRecord record = toRecord(step);
        if (step.id() == null) {
            mapper.insert(record);
            return Optional.ofNullable(mapper.findActiveByTaskIdAndStepKey(step.provisionTaskId(), step.stepKey()))
                    .map(this::toDomain)
                    .orElse(step);
        }
        mapper.update(record);
        return Optional.ofNullable(mapper.findActiveByTaskIdAndStepKey(step.provisionTaskId(), step.stepKey()))
                .map(this::toDomain)
                .orElse(step);
    }

    private TenantProvisionTaskStep toDomain(TenantProvisionTaskStepRecord record) {
        return new TenantProvisionTaskStep(
                record.id(),
                record.tenantId(),
                record.provisionTaskId(),
                record.stepKey(),
                record.stepName(),
                ProvisionTaskStepStatus.fromCode(record.status()),
                record.sequenceNo(),
                record.idempotencyKey(),
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

    private TenantProvisionTaskStepRecord toRecord(TenantProvisionTaskStep step) {
        return new TenantProvisionTaskStepRecord(
                step.id(),
                step.tenantId(),
                step.provisionTaskId(),
                step.stepKey(),
                step.stepName(),
                step.status().code(),
                step.sequenceNo(),
                step.idempotencyKey(),
                step.requestPayloadJson(),
                step.resultPayloadJson(),
                step.retryCount(),
                step.maxRetryCount(),
                step.lastErrorCode(),
                step.lastErrorMessage(),
                step.startedAt(),
                step.finishedAt(),
                step.createdBy(),
                step.createdAt(),
                step.updatedBy(),
                step.updatedAt(),
                step.deletedBy(),
                step.deleteReason(),
                step.deletedAt()
        );
    }
}
