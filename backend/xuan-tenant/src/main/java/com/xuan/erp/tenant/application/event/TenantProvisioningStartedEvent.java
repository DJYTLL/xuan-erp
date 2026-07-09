package com.xuan.erp.tenant.application.event;

import java.time.OffsetDateTime;

public record TenantProvisioningStartedEvent(
        Long tenantId,
        Long taskId,
        String taskKey,
        String idempotencyKey,
        String requestedBy,
        OffsetDateTime occurredAt
) {
}
