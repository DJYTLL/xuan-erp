package com.xuan.erp.iam.application.command;

/**
 * 保存角色列权限模板绑定命令。
 */
public record SetIamRoleColumnPermissionTemplateCommand(
        Long tenantId,
        Long roleId,
        Long templateId,
        String operator
) {
}
