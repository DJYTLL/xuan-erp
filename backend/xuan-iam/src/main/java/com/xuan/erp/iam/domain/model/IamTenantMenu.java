package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 租户菜单授权领域模型，表示租户可访问的全局菜单范围。
 */
public record IamTenantMenu(
        Long id,
        Long tenantId,
        Long menuId,
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
