package com.xuan.erp.iam.domain.model;

/**
 * IAM 租户可用列权限模板分配。
 */
public record IamTenantColumnPermissionTemplateAssignment(
        Long tenantId,
        Long templateId,
        String templateCode,
        String templateName,
        String templateDescription,
        boolean defaultTemplate,
        boolean templateEnabled) {
}
