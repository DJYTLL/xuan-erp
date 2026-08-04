package com.xuan.erp.iam.application.command;

/**
 * 新增 IAM 列权限模板命令。
 */
public record CreateIamColumnPermissionTemplateCommand(
        Long tenantId,
        String code,
        String name,
        String description,
        Boolean enabled,
        String operator
) {
}
