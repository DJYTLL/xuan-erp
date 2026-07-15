package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IAM 租户初始化权限模板，定义新租户初始化时可复用的权限编码集合。
 */
public record IamTenantInitPermissionTemplate(
        Long id,
        String code,
        String name,
        String description,
        List<String> permissionCodes,
        boolean defaultTemplate,
        boolean enabled,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
