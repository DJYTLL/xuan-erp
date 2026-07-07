package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 角色领域模型，表示租户内可分配给用户的权限集合载体。
 */
public record IamRole(
        Long id,
        Long tenantId,
        String code,
        String name,
        String description,
        boolean enabled,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public boolean active() {
        return deletedAt == null;
    }
}
