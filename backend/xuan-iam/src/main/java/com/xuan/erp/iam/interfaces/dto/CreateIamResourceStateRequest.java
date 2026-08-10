package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建 IAM 资源状态请求体。
 */
@Schema(description = "创建 IAM 资源状态请求体")
public record CreateIamResourceStateRequest(
        @Schema(description = "租户 ID，0 表示平台通用状态", example = "0")
        Long tenantId,
        @Schema(description = "资源标识", example = "sales-order")
        String resourceKey,
        @Schema(description = "状态编码", example = "DRAFT")
        String stateCode,
        @Schema(description = "状态名称", example = "草稿")
        String stateName,
        @Schema(description = "状态说明", example = "销售订单草稿状态")
        String description,
        @Schema(description = "排序号", example = "10")
        Integer sortNo,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled,
        @Schema(description = "扩展元数据 JSON", example = "{\"tagType\":\"info\"}")
        String metadataJson,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
