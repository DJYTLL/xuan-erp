package com.xuan.erp.tenant;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.RocketMqSendResult;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.infrastructure.mq.TenantOutboxPublisher;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantOutboxPublisherTest {

    @Test
    void publishesPendingOutboxEventAndMarksItPublished() {
        InMemoryTenantOutboxEventRepository repository = new InMemoryTenantOutboxEventRepository();
        RecordingRocketMqMessageSender sender = new RecordingRocketMqMessageSender();
        TenantOutboxPublisher publisher = new TenantOutboxPublisher(repository, sender);
        TenantOutboxEvent event = repository.append(new TenantOutboxEvent(
                null,
                101L,
                "tenant-event-1",
                "TENANT_PROVISION_TASK",
                9001L,
                "TenantIamBootstrapRequested",
                "xuan-tenant-event",
                "{\"tenantId\":101,\"eventType\":\"TenantIamBootstrapRequested\"}",
                "{\"sourceService\":\"xuan-tenant\"}",
                OutboxEventStatus.PENDING,
                0,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "system",
                OffsetDateTime.parse("2026-07-15T08:00:00Z"),
                "system",
                OffsetDateTime.parse("2026-07-15T08:00:00Z")
        ));

        int published = publisher.publishPending(10);

        assertThat(published).isEqualTo(1);
        assertThat(sender.lastMessage).isNotNull();
        assertThat(sender.lastMessage.topic()).isEqualTo("xuan-tenant-event");
        assertThat(sender.lastMessage.tag()).isEqualTo("TenantIamBootstrapRequested");
        assertThat(sender.lastMessage.key()).isEqualTo("tenant-event-1");
        assertThat(sender.lastMessage.payload()).isEqualTo("{\"tenantId\":101,\"eventType\":\"TenantIamBootstrapRequested\"}");
        TenantOutboxEvent saved = repository.findById(event.id()).orElseThrow();
        assertThat(saved.status()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(saved.publishedAt()).isNotNull();
        assertThat(saved.lastErrorCode()).isNull();
    }

    @Test
    void marksOutboxEventFailedWhenMessageSendFails() {
        InMemoryTenantOutboxEventRepository repository = new InMemoryTenantOutboxEventRepository();
        RecordingRocketMqMessageSender sender = new RecordingRocketMqMessageSender();
        sender.fail = true;
        TenantOutboxPublisher publisher = new TenantOutboxPublisher(repository, sender);
        TenantOutboxEvent event = repository.append(new TenantOutboxEvent(
                null, 101L, "tenant-event-2", "TENANT_PROVISION_TASK", 9001L,
                "TenantIamBootstrapRequested", "xuan-tenant-event", "{}", "{}",
                OutboxEventStatus.PENDING, 0, 5, null, null, null, null, null,
                null, null, null, null, "system", OffsetDateTime.parse("2026-07-15T08:00:00Z"),
                "system", OffsetDateTime.parse("2026-07-15T08:00:00Z")
        ));

        int published = publisher.publishPending(10);

        assertThat(published).isZero();
        TenantOutboxEvent saved = repository.findById(event.id()).orElseThrow();
        assertThat(saved.status()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(saved.retryCount()).isEqualTo(1);
        assertThat(saved.lastErrorCode()).isEqualTo("ROCKETMQ_SEND_FAILED");
        assertThat(saved.nextRetryAt()).isNotNull();
    }

    static class RecordingRocketMqMessageSender implements RocketMqMessageSender {

        private RocketMqMessage<?> lastMessage;
        private boolean fail;

        @Override
        public <T> RocketMqSendResult send(RocketMqMessage<T> message) {
            if (fail) {
                throw new IllegalStateException("mq down");
            }
            this.lastMessage = message;
            return new RocketMqSendResult("SEND_OK", "msg-tenant-outbox", 0, 0L);
        }
    }

    static class InMemoryTenantOutboxEventRepository implements TenantOutboxEventRepository {
        private final Map<Long, TenantOutboxEvent> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<TenantOutboxEvent> findById(Long eventId) {
            return Optional.ofNullable(store.get(eventId));
        }

        @Override
        public List<TenantOutboxEvent> findPublishable(int limit) {
            return store.values().stream()
                    .filter(event -> event.status() == OutboxEventStatus.PENDING || event.status() == OutboxEventStatus.FAILED)
                    .filter(event -> event.retryCount() < event.maxRetryCount())
                    .limit(limit)
                    .toList();
        }

        @Override
        public TenantOutboxEvent append(TenantOutboxEvent event) {
            return save(event);
        }

        @Override
        public TenantOutboxEvent save(TenantOutboxEvent event) {
            Long id = event.id() == null ? nextId++ : event.id();
            TenantOutboxEvent saved = new TenantOutboxEvent(
                    id,
                    event.tenantId(),
                    event.eventId(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.eventType(),
                    event.topic(),
                    event.payloadJson(),
                    event.headersJson(),
                    event.status(),
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
            store.put(id, saved);
            return saved;
        }
    }
}
