package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import java.time.OffsetDateTime;

public record TenantProvisionTaskStep(
        Long id,
        Long tenantId,
        Long provisionTaskId,
        String stepKey,
        String stepName,
        ProvisionTaskStepStatus status,
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
