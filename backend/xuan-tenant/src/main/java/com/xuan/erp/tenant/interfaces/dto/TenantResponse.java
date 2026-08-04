package com.xuan.erp.tenant.interfaces.dto;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;

public record TenantResponse(
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
        String permissionSyncStatus,
        String permissionSyncStatusLabel,
        OffsetDateTime permissionSyncLastCheckedAt,
        OffsetDateTime permissionSyncLastSyncedAt,
        String permissionSyncLastErrorMessage,
        long statusHistoryCount,
        String latestStatusChangeType,
        OffsetDateTime latestStatusChangedAt
) {

    public TenantResponse(
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
                null, null, null, null, null, null, null, null, null, null, null, null, 0L, null, null);
    }

    public TenantResponse(
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
            long statusHistoryCount,
            String latestStatusChangeType,
            OffsetDateTime latestStatusChangedAt
    ) {
        this(id, code, name, status, contactName, contactPhone, provisionedAt, enabledAt, remark,
                currentPlanAssignmentId, currentPlanId, currentPlanCode, currentPlanName, currentPlanExpiresAt,
                primaryDomainId, primaryDomain, null, null, null, null, null,
                statusHistoryCount, latestStatusChangeType, latestStatusChangedAt);
    }
}
