package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import java.time.OffsetDateTime;

public record TenantOutboxEvent(
        Long id,
        Long tenantId,
        String eventId,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String topic,
        String payloadJson,
        String headersJson,
        OutboxEventStatus status,
        int retryCount,
        int maxRetryCount,
        String lockedBy,
        OffsetDateTime lockedAt,
        OffsetDateTime lockExpiresAt,
        OffsetDateTime nextRetryAt,
        OffsetDateTime publishedAt,
        String lastErrorCode,
        String lastErrorMessage,
        OffsetDateTime firstFailedAt,
        OffsetDateTime deadLetterAt,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
