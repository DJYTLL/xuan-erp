package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantOutboxEventRecord;
import com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantOutboxEventPersistenceMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantOutboxEventRepositoryAdapter implements TenantOutboxEventRepository {

    private final TenantOutboxEventPersistenceMapper mapper;

    public TenantOutboxEventRepositoryAdapter(TenantOutboxEventPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<TenantOutboxEvent> findById(Long eventId) {
        return Optional.ofNullable(mapper.findById(eventId)).map(this::toDomain);
    }

    @Override
    public List<TenantOutboxEvent> findPublishable(int limit) {
        return mapper.findPublishable(OffsetDateTime.now(), Math.max(limit, 1)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TenantOutboxEvent append(TenantOutboxEvent event) {
        return save(event);
    }

    @Override
    public TenantOutboxEvent save(TenantOutboxEvent event) {
        TenantOutboxEventRecord record = toRecord(event);
        if (event.id() == null) {
            mapper.insert(record);
            return Optional.ofNullable(mapper.findLatestByEventId(event.eventId()))
                    .map(this::toDomain)
                    .orElse(event);
        }
        mapper.update(record);
        return Optional.ofNullable(mapper.findById(event.id()))
                .map(this::toDomain)
                .orElse(event);
    }

    private TenantOutboxEvent toDomain(TenantOutboxEventRecord record) {
        return new TenantOutboxEvent(
                record.id(),
                record.tenantId(),
                record.eventId(),
                record.aggregateType(),
                record.aggregateId(),
                record.eventType(),
                record.topic(),
                record.payloadJson(),
                record.headersJson(),
                OutboxEventStatus.fromCode(record.status()),
                record.retryCount(),
                record.maxRetryCount(),
                record.lockedBy(),
                record.lockedAt(),
                record.lockExpiresAt(),
                record.nextRetryAt(),
                record.publishedAt(),
                record.lastErrorCode(),
                record.lastErrorMessage(),
                record.firstFailedAt(),
                record.deadLetterAt(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt()
        );
    }

    private TenantOutboxEventRecord toRecord(TenantOutboxEvent event) {
        return new TenantOutboxEventRecord(
                event.id(),
                event.tenantId(),
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.topic(),
                event.payloadJson(),
                event.headersJson(),
                event.status().code(),
                event.retryCount(),
                event.maxRetryCount(),
                event.lockedBy(),
                event.lockedAt(),
                event.lockExpiresAt(),
                event.nextRetryAt(),
                event.publishedAt(),
                event.lastErrorCode(),
                event.lastErrorMessage(),
                event.firstFailedAt(),
                event.deadLetterAt(),
                event.createdBy(),
                event.createdAt(),
                event.updatedBy(),
                event.updatedAt()
        );
    }
}
