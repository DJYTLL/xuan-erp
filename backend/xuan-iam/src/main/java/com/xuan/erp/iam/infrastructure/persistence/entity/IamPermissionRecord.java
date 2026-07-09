package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 权限持久化记录，表达 iam_permission 表字段在基础设施层的结构。
 */
public record IamPermissionRecord(
        Long id,
        String code,
        String name,
        String serviceName,
        String menuCode,
        String description,
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
