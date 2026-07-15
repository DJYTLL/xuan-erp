package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 新增租户初始化权限模板命令，承载模板基础信息和初始权限编码集合。
 */
public record CreateIamTenantInitTemplateCommand(
        String code,
        String name,
        String description,
        Boolean enabled,
        Boolean defaultTemplate,
        List<String> permissionCodes
) {
}
