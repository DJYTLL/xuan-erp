package com.xuan.erp.iam.application.query;

import java.util.List;

/**
 * IAM 角色权限授权视图，返回角色当前拥有的权限编码集合。
 */
public record IamRolePermissionGrantView(
        Long tenantId,
        Long roleId,
        List<String> permissionCodes
) {
}
