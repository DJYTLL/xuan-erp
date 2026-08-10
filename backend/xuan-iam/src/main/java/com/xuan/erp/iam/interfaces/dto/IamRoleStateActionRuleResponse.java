package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 角色状态动作规则响应 DTO。
 */
@Schema(description = "IAM 角色状态动作规则响应 DTO")
public record IamRoleStateActionRuleResponse(
        @Schema(description = "规则 ID", example = "1")
        Long id,
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "角色 ID", example = "10")
        Long roleId,
        @Schema(description = "资源标识", example = "sales-order")
        String resourceKey,
        @Schema(description = "状态编码", example = "DRAFT")
        String stateCode,
        @Schema(description = "动作编码", example = "submit")
        String actionCode,
        @Schema(description = "是否启用", example = "true")
        boolean enabled
) {
}
