package com.xuan.erp.tenant.domain.model;

import java.time.OffsetDateTime;

/**
 * 租户详情页需要的聚合补充信息。
 */
public record TenantDetailSupplement(
        Long currentPlanId,
        String currentPlanCode,
        String currentPlanName,
        Long primaryDomainId,
        String primaryDomain,
        long statusHistoryCount,
        String latestStatusChangeType,
        OffsetDateTime latestStatusChangedAt
) {

    public static TenantDetailSupplement empty() {
        return new TenantDetailSupplement(null, null, null, null, null, 0L, null, null);
    }
}
