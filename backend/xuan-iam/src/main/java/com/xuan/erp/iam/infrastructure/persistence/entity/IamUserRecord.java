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
        Boolean enabled,
        Boolean accountNonExpired,
        Boolean accountNonLocked,
        Boolean credentialsNonExpired,
        OffsetDateTime lastLoginAt,
        OffsetDateTime passwordChangedAt,
        Integer failedLoginCount,
        OffsetDateTime lastFailedLoginAt,
        OffsetDateTime lockedUntil,
        Boolean mfaEnabled,
        Long authVersion,
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
