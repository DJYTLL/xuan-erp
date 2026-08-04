package com.xuan.erp.iam.domain.model;

/**
 * IAM 角色列权限规则。
 */
public record IamRoleColumnPermissionRule(
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
