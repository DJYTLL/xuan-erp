package com.xuan.erp.tenant.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record TenantProvisionTaskRecord(
        Long id,
        Long tenantId,
        String taskKey,
        String taskType,
        String status,
        String idempotencyKey,
        String stepName,
        String requestPayloadJson,
        String resultPayloadJson,
        int retryCount,
        int maxRetryCount,
        String lastErrorCode,
        String lastErrorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
