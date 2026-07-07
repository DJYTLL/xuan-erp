package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import java.time.OffsetDateTime;

public record TenantProvisionTask(
        Long id,
        Long tenantId,
        String taskKey,
        String taskType,
        ProvisionTaskStatus status,
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
