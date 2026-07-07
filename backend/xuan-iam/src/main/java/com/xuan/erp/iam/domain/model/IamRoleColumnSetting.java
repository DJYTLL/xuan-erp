package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IAM 角色列权限设置领域模型，记录角色在页面上的可见列范围。
 */
public record IamRoleColumnSetting(
        Long id,
        Long tenantId,
        Long roleId,
        String pageKey,
        List<String> visibleColumns,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
