package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改 IAM 资源状态请求体。
 */
@Schema(description = "修改 IAM 资源状态请求体")
public record UpdateIamResourceStateRequest(
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
