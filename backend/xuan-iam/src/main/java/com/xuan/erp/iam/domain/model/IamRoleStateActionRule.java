package com.xuan.erp.iam.domain.model;

/**
 * IAM 角色状态动作规则，表示租户角色在某个资源状态下允许执行的动作。
 */
public record IamRoleStateActionRule(
        Long id,
        Long tenantId,
        Long roleId,
        String resourceKey,
        String stateCode,
        String actionCode,
        boolean enabled
) {
}
