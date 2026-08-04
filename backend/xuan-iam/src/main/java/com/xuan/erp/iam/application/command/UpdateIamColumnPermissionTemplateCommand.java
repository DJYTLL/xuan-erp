package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 列权限模板命令。
 */
public record UpdateIamColumnPermissionTemplateCommand(
        String name,
        String description,
        Boolean enabled,
        String operator
) {
}
