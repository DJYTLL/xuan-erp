package com.xuan.erp.tenant.infrastructure.rpc;

/**
 * IAM 租户权限同步状态响应。
 */
public record IamTenantPermissionSyncStateResponse(
        Long tenantId,
        String lastSyncedPermissionHash
) {
}
