package com.xuan.erp.iam.interfaces.dto;

/**
 * IAM 角色列权限字段规则响应。
 */
public record IamRoleColumnPermissionRuleResponse(
        Long id,
        Long tenantId,
        Long roleId,
        Long resourceColumnId,
        String resourceKey,
        String columnKey,
        String columnName,
        String accessMode
) {
}
