package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 用户持久化记录，表达 iam_user 表字段在基础设施层的结构。
 */
public record IamUserRecord(
        Long id,
        Long tenantId,
        String username,
        String passwordHash,
        String displayName,
        String email,
        String phone,
        String avatarUrl,
        boolean enabled,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        OffsetDateTime lastLoginAt,
        OffsetDateTime passwordChangedAt,
        int failedLoginCount,
        OffsetDateTime lastFailedLoginAt,
        OffsetDateTime lockedUntil,
        boolean mfaEnabled,
        long authVersion,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
