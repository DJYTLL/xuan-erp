package com.xuan.erp.iam.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * IAM 用户通用偏好持久化记录，表达 iam_user_preference 表字段。
 */
public record IamUserPreferenceRecord(
        Long id,
        Long tenantId,
        Long userId,
        String preferenceKey,
        String preferenceJson,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
