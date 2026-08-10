package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 设置 IAM 角色状态动作规则命令。
 */
public record SetIamRoleStateActionRulesCommand(
        Long tenantId,
        Long roleId,
        List<IamRoleStateActionRuleCommand> rules,
        String operator
) {
}
