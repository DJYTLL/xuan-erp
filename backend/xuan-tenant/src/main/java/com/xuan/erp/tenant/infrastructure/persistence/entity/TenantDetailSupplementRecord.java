package com.xuan.erp.tenant.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record TenantDetailSupplementRecord(
        Long currentPlanId,
        String currentPlanCode,
        String currentPlanName,
        Long primaryDomainId,
        String primaryDomain,
        Long statusHistoryCount,
        String latestStatusChangeType,
        OffsetDateTime latestStatusChangedAt
) {
}
