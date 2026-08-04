package com.xuan.erp.tenant.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

public record TenantDetailSupplementRecord(
        Long currentPlanAssignmentId,
        Long currentPlanId,
        String currentPlanCode,
        String currentPlanName,
        OffsetDateTime currentPlanExpiresAt,
        Long primaryDomainId,
        String primaryDomain,
        String permissionSyncExpectedHash,
        String permissionSyncStatus,
        OffsetDateTime permissionSyncLastCheckedAt,
        OffsetDateTime permissionSyncLastSyncedAt,
        String permissionSyncLastErrorCode,
        String permissionSyncLastErrorMessage,
        Long statusHistoryCount,
        String latestStatusChangeType,
        OffsetDateTime latestStatusChangedAt
) {
}
