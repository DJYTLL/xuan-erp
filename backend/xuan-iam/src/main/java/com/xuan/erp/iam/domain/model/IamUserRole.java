package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 用户角色关联领域模型，表示用户在租户内拥有的角色。
 */
public record IamUserRole(
        Long id,
        Long tenantId,
        Long userId,
        Long roleId,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
