package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 用户领域模型，表示租户内登录账号及其安全状态。
 */
public record IamUser(
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

    public static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("iam username must not be blank");
        }
        return username.trim();
    }

    public boolean active() {
        return deletedAt == null;
    }
}
