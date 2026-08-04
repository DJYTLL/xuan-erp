package com.xuan.erp.iam.domain.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * IAM 侧租户套餐权限同步状态仓储。
 */
public interface IamTenantPermissionSyncStateRepository {

    Optional<String> findLastSyncedPermissionHash(Long tenantId);

    void markSynced(Long tenantId, String permissionHash, String source, OffsetDateTime syncedAt);
}
