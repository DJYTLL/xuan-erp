package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 保存租户可用列权限模板命令。
 */
public record SetIamTenantColumnPermissionTemplatesCommand(
        Long tenantId,
        List<Long> templateIds,
        Long defaultTemplateId,
        String operator) {
}
