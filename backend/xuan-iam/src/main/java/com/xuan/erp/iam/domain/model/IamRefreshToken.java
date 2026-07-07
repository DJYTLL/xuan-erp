package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 刷新令牌领域模型，记录刷新令牌轮换、撤销和设备上下文。
 */
public record IamRefreshToken(
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
