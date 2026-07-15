package com.xuan.erp.tenant.infrastructure.mq;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 租户 Outbox 发布器，负责把待发布事件发送到 RocketMQ 并回写发布状态。
 */
@Component
@ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
public class TenantOutboxPublisher {

    private static final int DEFAULT_BATCH_SIZE = 50;

    private final TenantOutboxEventRepository repository;
    private final RocketMqMessageSender sender;

    public TenantOutboxPublisher(TenantOutboxEventRepository repository, RocketMqMessageSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${xuan.tenant.outbox.publish-delay:5000}")
    public void publishScheduled() {
        publishPending(DEFAULT_BATCH_SIZE);
    }

    public int publishPending(int limit) {
        int published = 0;
        for (TenantOutboxEvent event : repository.findPublishable(limit)) {
            if (publish(event)) {
                published++;
            }
        }
        return published;
    }

    private boolean publish(TenantOutboxEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        repository.save(copy(event, OutboxEventStatus.PUBLISHING, event.retryCount(), null, null, null, null, now));
        try {
            sender.send(new RocketMqMessage<>(
                    event.topic(),
                    event.eventType(),
                    event.eventId(),
                    event.payloadJson(),
                    Map.of("sourceService", "xuan-tenant", "eventId", event.eventId())));
            repository.save(copy(event, OutboxEventStatus.PUBLISHED, event.retryCount(), null, null, null, now, now));
            return true;
        } catch (RuntimeException error) {
            int retryCount = event.retryCount() + 1;
            OutboxEventStatus status = retryCount >= event.maxRetryCount()
                    ? OutboxEventStatus.DEAD_LETTERED
                    : OutboxEventStatus.FAILED;
            repository.save(copy(
                    event,
                    status,
                    retryCount,
                    "ROCKETMQ_SEND_FAILED",
                    error.getMessage(),
                    event.firstFailedAt() == null ? now : event.firstFailedAt(),
                    status == OutboxEventStatus.DEAD_LETTERED ? now : null,
                    now));
            return false;
        }
    }

    private TenantOutboxEvent copy(
            TenantOutboxEvent event,
            OutboxEventStatus status,
            int retryCount,
            String errorCode,
            String errorMessage,
            OffsetDateTime firstFailedAt,
            OffsetDateTime publishedOrDeadLetterAt,
            OffsetDateTime now) {
        OffsetDateTime nextRetryAt = status == OutboxEventStatus.FAILED ? now.plusSeconds(30) : null;
        OffsetDateTime publishedAt = status == OutboxEventStatus.PUBLISHED ? publishedOrDeadLetterAt : event.publishedAt();
        OffsetDateTime deadLetterAt = status == OutboxEventStatus.DEAD_LETTERED ? publishedOrDeadLetterAt : event.deadLetterAt();
        return new TenantOutboxEvent(
                event.id(),
                event.tenantId(),
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.topic(),
                event.payloadJson(),
                event.headersJson(),
                status,
                retryCount,
                event.maxRetryCount(),
                null,
                null,
                null,
                nextRetryAt,
                publishedAt,
                errorCode,
                errorMessage,
                firstFailedAt,
                deadLetterAt,
                event.createdBy(),
                event.createdAt(),
                "tenant-outbox-publisher",
                now
        );
    }
}
