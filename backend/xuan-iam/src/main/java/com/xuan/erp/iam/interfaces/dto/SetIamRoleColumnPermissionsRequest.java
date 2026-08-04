package com.xuan.erp.iam.interfaces.dto;

import java.util.List;

/**
 * IAM 保存角色列权限规则请求。
 */
public record SetIamRoleColumnPermissionsRequest(
        Long tenantId,
        List<IamRoleColumnPermissionRuleRequest> rules,
        String operator
) {
}
