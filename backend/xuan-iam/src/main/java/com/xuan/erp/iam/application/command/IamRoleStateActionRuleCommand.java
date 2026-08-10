package com.xuan.erp.iam.application.command;

/**
 * IAM 角色状态动作规则命令项。
 */
public record IamRoleStateActionRuleCommand(
        String resourceKey,
        String stateCode,
        String actionCode
) {
}
