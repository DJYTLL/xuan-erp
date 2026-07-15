package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * 保存 IAM 用户通用偏好请求体。
 *
 * @param value 偏好内容
 */
@Schema(description = "保存 IAM 用户通用偏好请求体")
public record SaveIamUserPreferenceRequest(
        @Schema(description = "偏好内容")
        Map<String, Object> value
) {
}
