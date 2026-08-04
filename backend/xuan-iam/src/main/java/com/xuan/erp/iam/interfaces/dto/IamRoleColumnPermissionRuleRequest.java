package com.xuan.erp.iam.interfaces.dto;

/**
 * IAM 角色列权限字段规则请求。
 */
public record IamRoleColumnPermissionRuleRequest(
        Long resourceColumnId,
        String accessMode
) {
}
