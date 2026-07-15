package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;

/**
 * IAM 用户通用偏好领域模型，保存用户在前端框架和业务页面上的个性化配置。
 */
public record IamUserPreference(
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
