package com.xuan.erp.iam.interfaces.dto;

import java.util.List;

/**
 * 保存租户可用列权限模板请求。
 */
public record SetIamTenantColumnPermissionTemplatesRequest(
        List<Long> templateIds,
        Long defaultTemplateId,
        String operator) {
}
