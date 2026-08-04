package com.xuan.erp.tenant.domain.model;

import java.time.OffsetDateTime;

/**
 * 租户详情页需要的聚合补充信息。
 */
public record TenantDetailSupplement(
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
        long statusHistoryCount,
        String latestStatusChangeType,
        OffsetDateTime latestStatusChangedAt
) {

    public TenantDetailSupplement(
            Long currentPlanAssignmentId,
            Long currentPlanId,
            String currentPlanCode,
            String currentPlanName,
            OffsetDateTime currentPlanExpiresAt,
            Long primaryDomainId,
            String primaryDomain,
            long statusHistoryCount,
            String latestStatusChangeType,
            OffsetDateTime latestStatusChangedAt
    ) {
        this(currentPlanAssignmentId, currentPlanId, currentPlanCode, currentPlanName, currentPlanExpiresAt,
                primaryDomainId, primaryDomain, null, null, null, null, null, null,
                statusHistoryCount, latestStatusChangeType, latestStatusChangedAt);
    }

    public static TenantDetailSupplement empty() {
        return new TenantDetailSupplement(null, null, null, null, null, null, null,
                null, null, null, null, null, null, 0L, null, null);
    }
}
