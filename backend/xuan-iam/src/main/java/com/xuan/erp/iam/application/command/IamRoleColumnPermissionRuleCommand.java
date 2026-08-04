package com.xuan.erp.iam.application.command;

/**
 * IAM 角色列权限字段规则命令项。
 */
public record IamRoleColumnPermissionRuleCommand(
        Long resourceColumnId,
        String accessMode
) {
}
