package com.xuan.erp.iam.application.command;

/**
 * 修改租户初始化权限模板命令，负责模板名称、说明、启用状态和默认标识。
 */
public record UpdateIamTenantInitTemplateCommand(
        String name,
        String description,
        Boolean enabled,
        Boolean defaultTemplate
) {
}
