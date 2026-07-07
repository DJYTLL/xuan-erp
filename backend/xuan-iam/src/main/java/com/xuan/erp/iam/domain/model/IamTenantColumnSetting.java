package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IAM 租户列权限默认设置领域模型，记录租户层面的页面可见列默认值。
 */
public record IamTenantColumnSetting(
        Long id,
        Long tenantId,
        String pageKey,
        List<String> visibleColumns,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
