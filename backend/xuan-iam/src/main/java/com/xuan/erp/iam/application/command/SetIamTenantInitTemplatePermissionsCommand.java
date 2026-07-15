package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 替换租户初始化权限模板授权命令，使用权限编码集合表达最终模板权限。
 */
public record SetIamTenantInitTemplatePermissionsCommand(
        Long templateId,
        List<String> permissionCodes,
        String operator
) {
}
