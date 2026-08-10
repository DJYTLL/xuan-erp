package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 资源动作响应 DTO。
 */
@Schema(description = "IAM 资源动作响应 DTO")
public record IamResourceActionResponse(
        @Schema(description = "动作 ID", example = "1")
        Long id,
        @Schema(description = "租户 ID，0 表示平台通用动作", example = "0")
        Long tenantId,
        @Schema(description = "资源标识", example = "sales-order")
        String resourceKey,
        @Schema(description = "动作编码", example = "submit")
        String actionCode,
        @Schema(description = "动作名称", example = "提交")
        String actionName,
        @Schema(description = "对应基础权限码", example = "sales-order:update")
        String permissionCode,
        @Schema(description = "动作说明", example = "提交销售订单")
        String description,
        @Schema(description = "排序号", example = "20")
        Integer sortNo,
        @Schema(description = "是否启用", example = "true")
        boolean enabled,
        @Schema(description = "扩展元数据 JSON", example = "{\"danger\":false}")
        String metadataJson
) {
}
