package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 授权快照持久化记录，表达 iam_authorization_snapshot 表字段在基础设施层的结构。
 */
public record IamAuthorizationSnapshotRecord(
        Long id,
        Long tenantId,
        Long userId,
        Long authVersion,
        String roleIdsJson,
        String permissionCodesJson,
        String menuCodesJson,
        String columnSettingsJson,
        String snapshotHash,
        OffsetDateTime expiresAt,
        OffsetDateTime builtAt,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
