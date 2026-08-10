package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 保存 IAM 角色状态动作规则请求体。
 */
@Schema(description = "保存 IAM 角色状态动作规则请求体")
public record SetIamRoleStateActionRulesRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "状态动作规则集合")
        List<IamRoleStateActionRuleRequest> rules,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
