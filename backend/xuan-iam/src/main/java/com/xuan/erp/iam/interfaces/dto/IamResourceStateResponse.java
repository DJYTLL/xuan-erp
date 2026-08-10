package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 资源状态响应 DTO。
 */
@Schema(description = "IAM 资源状态响应 DTO")
public record IamResourceStateResponse(
        @Schema(description = "状态 ID", example = "1")
        Long id,
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
        boolean enabled,
        @Schema(description = "扩展元数据 JSON", example = "{\"tagType\":\"info\"}")
        String metadataJson
) {
}
