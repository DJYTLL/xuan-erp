package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 角色状态动作规则请求项。
 */
@Schema(description = "IAM 角色状态动作规则请求项")
public record IamRoleStateActionRuleRequest(
        @Schema(description = "资源标识", example = "sales-order")
        String resourceKey,
        @Schema(description = "状态编码", example = "DRAFT")
        String stateCode,
        @Schema(description = "动作编码", example = "submit")
        String actionCode
) {
}
