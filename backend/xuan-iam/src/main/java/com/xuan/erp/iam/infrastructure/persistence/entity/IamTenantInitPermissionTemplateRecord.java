package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 租户初始化权限模板持久化记录，映射 iam_tenant_init_permission_template 表字段。
 */
public record IamTenantInitPermissionTemplateRecord(
        Long id,
        String code,
        String name,
        String description,
        String permissionCodesJson,
        Boolean defaultTemplate,
        Boolean enabled,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
