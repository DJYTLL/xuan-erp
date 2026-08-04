package com.xuan.erp.iam.domain.model;

/**
 * IAM 角色列权限模板绑定领域模型。
 */
public record IamRoleColumnPermissionTemplateBinding(
        Long tenantId,
        Long roleId,
        Long templateId,
        String templateCode,
        String templateName
) {
}
