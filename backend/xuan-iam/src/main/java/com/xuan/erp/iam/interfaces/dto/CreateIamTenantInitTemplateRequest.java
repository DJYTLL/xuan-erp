package com.xuan.erp.iam.interfaces.dto;

import java.util.List;

/**
 * 新增租户初始化权限模板请求 DTO，承载页面提交的模板资料。
 */
public record CreateIamTenantInitTemplateRequest(
        String code,
        String name,
        String description,
        Boolean enabled,
        Boolean defaultTemplate,
        List<String> permissionCodes
) {
}
