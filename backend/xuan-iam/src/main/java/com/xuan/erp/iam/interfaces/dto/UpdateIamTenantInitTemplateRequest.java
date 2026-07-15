package com.xuan.erp.iam.interfaces.dto;

/**
 * 修改租户初始化权限模板请求 DTO，承载模板资料编辑结果。
 */
public record UpdateIamTenantInitTemplateRequest(
        String name,
        String description,
        Boolean enabled,
        Boolean defaultTemplate
) {
}
