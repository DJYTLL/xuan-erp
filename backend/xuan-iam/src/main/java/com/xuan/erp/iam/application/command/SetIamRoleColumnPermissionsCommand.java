package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * IAM 保存角色列权限规则命令。
 */
public record SetIamRoleColumnPermissionsCommand(
        Long tenantId,
        Long roleId,
        List<IamRoleColumnPermissionRuleCommand> rules,
        String operator
) {
}
