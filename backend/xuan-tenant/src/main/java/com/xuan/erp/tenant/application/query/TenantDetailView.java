package com.xuan.erp.tenant.application.query;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;

public record TenantDetailView(
        Long id,
        String code,
        String name,
        TenantStatus status,
        String contactName,
        String contactPhone,
        OffsetDateTime provisionedAt,
        OffsetDateTime enabledAt,
        String remark,
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

    public TenantDetailView(
            Long id,
            String code,
            String name,
            TenantStatus status,
            String contactName,
            String contactPhone,
            OffsetDateTime provisionedAt,
            OffsetDateTime enabledAt,
            String remark
    ) {
        this(id, code, name, status, contactName, contactPhone, provisionedAt, enabledAt, remark,
                null, null, null, null, null, null, null, null, null, null, null, null, null, 0L, null, null);
    }
}
