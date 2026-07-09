package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 角色持久化记录，表达 iam_role 表字段在基础设施层的结构。
 */
public record IamRoleRecord(
        Long id,
        Long tenantId,
        String code,
        String name,
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
