package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * IAM 用户表格个性化设置领域模型，保存用户在指定页面的表格偏好。
 */
public record IamUserTableSetting(
        Long id,
        Long tenantId,
        Long userId,
        String pageKey,
        Map<String, Object> config,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
