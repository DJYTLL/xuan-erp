package com.xuan.erp.iam.interfaces.dto;

/**
 * 租户可用列权限模板分配响应。
 */
public record IamTenantColumnPermissionTemplateAssignmentResponse(
        Long tenantId,
        Long templateId,
        String templateCode,
        String templateName,
        String templateDescription,
        boolean defaultTemplate,
        boolean templateEnabled) {
}
