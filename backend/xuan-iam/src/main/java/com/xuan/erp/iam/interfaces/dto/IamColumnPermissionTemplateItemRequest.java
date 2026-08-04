package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 列权限模板规则请求项。
 */
@Schema(description = "IAM 列权限模板规则请求项")
public record IamColumnPermissionTemplateItemRequest(
        @Schema(description = "资源字段 ID", example = "1")
        Long resourceColumnId,
        @Schema(description = "访问级别：VISIBLE 明文、MASKED 脱敏、HIDDEN 隐藏", example = "MASKED")
        String accessMode
) {
}
