package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * iam_refresh_token 表持久化记录。
 */
public record IamRefreshTokenRecord(
        Long id,
        Long tenantId,
        Long userId,
        String tokenHash,
        String tokenFamilyId,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        String replacedByTokenHash,
        OffsetDateTime lastUsedAt,
        Long audienceTenantId,
        String deviceId,
        String ipAddress,
        String userAgent,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
