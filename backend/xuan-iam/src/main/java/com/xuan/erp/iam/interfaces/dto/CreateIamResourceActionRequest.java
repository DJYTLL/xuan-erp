package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建 IAM 资源动作请求体。
 */
@Schema(description = "创建 IAM 资源动作请求体")
public record CreateIamResourceActionRequest(
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
        Boolean enabled,
        @Schema(description = "扩展元数据 JSON", example = "{\"danger\":false}")
        String metadataJson,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
