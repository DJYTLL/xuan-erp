package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 列权限模板规则响应 DTO。
 */
@Schema(description = "IAM 列权限模板规则响应 DTO")
public record IamColumnPermissionTemplateItemResponse(
        @Schema(description = "规则 ID", example = "100")
        Long id,
        @Schema(description = "模板 ID", example = "8")
        Long templateId,
        @Schema(description = "资源字段 ID", example = "1")
        Long resourceColumnId,
        @Schema(description = "资源标识", example = "tenant")
        String resourceKey,
        @Schema(description = "字段标识", example = "contactPhone")
        String columnKey,
        @Schema(description = "字段名称", example = "联系电话")
        String columnName,
        @Schema(description = "访问级别：VISIBLE 明文、MASKED 脱敏、HIDDEN 隐藏", example = "MASKED")
        String accessMode
) {
}
