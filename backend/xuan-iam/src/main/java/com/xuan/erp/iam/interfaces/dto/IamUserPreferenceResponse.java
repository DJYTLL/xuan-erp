package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * IAM 用户通用偏好响应体。
 *
 * @param preferenceKey 偏好键
 * @param value 偏好内容
 * @param updatedAt 更新时间
 */
@Schema(description = "IAM 用户通用偏好响应体")
public record IamUserPreferenceResponse(
        @Schema(description = "偏好键", example = "shell.layout")
        String preferenceKey,
        @Schema(description = "偏好内容")
        Map<String, Object> value,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
