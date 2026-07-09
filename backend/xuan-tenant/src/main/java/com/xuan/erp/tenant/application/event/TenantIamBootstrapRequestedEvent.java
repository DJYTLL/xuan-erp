package com.xuan.erp.tenant.application.event;

import java.time.OffsetDateTime;

public record TenantIamBootstrapRequestedEvent(
        Long tenantId,
        Long taskId,
        Long stepId,
        String stepKey,
        String requestedBy,
        OffsetDateTime occurredAt
) {
}
