package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 资源字段响应 DTO。
 */
@Schema(description = "IAM 资源字段响应 DTO")
public record IamResourceColumnResponse(
        @Schema(description = "资源字段 ID", example = "1")
        Long id,
        @Schema(description = "资源标识", example = "tenant")
        String resourceKey,
        @Schema(description = "字段标识", example = "contactPhone")
        String columnKey,
        @Schema(description = "字段名称", example = "联系电话")
        String columnName,
        @Schema(description = "数据类型", example = "STRING")
        String dataType,
        @Schema(description = "脱敏类型", example = "PHONE")
        String maskType,
        @Schema(description = "是否启用", example = "true")
        boolean enabled,
        @Schema(description = "排序", example = "50")
        Integer sortNo
) {
}
