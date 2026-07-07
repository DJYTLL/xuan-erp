package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 角色权限关联领域模型，表示角色与权限定义之间的授权关系。
 */
public record IamRolePermission(
        Long id,
        Long tenantId,
        Long roleId,
        Long permissionId,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
