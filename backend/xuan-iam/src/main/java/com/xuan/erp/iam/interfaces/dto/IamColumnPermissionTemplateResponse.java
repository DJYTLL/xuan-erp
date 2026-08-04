package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 列权限模板响应 DTO。
 */
@Schema(description = "IAM 列权限模板响应 DTO")
public record IamColumnPermissionTemplateResponse(
        @Schema(description = "模板 ID", example = "8")
        Long id,
        @Schema(description = "租户 ID，0 表示平台默认模板", example = "1001")
        Long tenantId,
        @Schema(description = "模板编码", example = "tenant_readonly_masked")
        String code,
        @Schema(description = "模板名称", example = "租户只读脱敏模板")
        String name,
        @Schema(description = "模板说明", example = "隐藏备注，联系电话脱敏")
        String description,
        @Schema(description = "是否启用", example = "true")
        boolean enabled
) {
}
