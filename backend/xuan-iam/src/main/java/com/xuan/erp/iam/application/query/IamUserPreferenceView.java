package com.xuan.erp.iam.application.query;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * IAM 用户通用偏好查询视图，返回前端可直接消费的偏好内容。
 */
public record IamUserPreferenceView(
        String preferenceKey,
        Map<String, Object> value,
        OffsetDateTime updatedAt
) {
}
