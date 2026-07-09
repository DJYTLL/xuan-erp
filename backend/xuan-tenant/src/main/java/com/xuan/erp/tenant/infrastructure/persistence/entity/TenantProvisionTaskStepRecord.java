package com.xuan.erp.tenant.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record TenantProvisionTaskStepRecord(
        Long id,
        Long tenantId,
        Long provisionTaskId,
        String stepKey,
        String stepName,
        String status,
        int sequenceNo,
        String idempotencyKey,
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
