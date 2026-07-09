package com.xuan.erp.tenant.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record TenantOutboxEventRecord(
        Long id,
        Long tenantId,
        String eventId,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String topic,
        String payloadJson,
        String headersJson,
        String status,
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
