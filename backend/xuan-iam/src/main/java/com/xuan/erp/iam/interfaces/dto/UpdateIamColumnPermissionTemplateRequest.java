package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改 IAM 列权限模板请求体。
 */
@Schema(description = "修改 IAM 列权限模板请求体")
public record UpdateIamColumnPermissionTemplateRequest(
        @Schema(description = "模板名称", example = "租户只读脱敏模板")
        String name,
        @Schema(description = "模板说明", example = "隐藏备注，联系电话脱敏")
        String description,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
