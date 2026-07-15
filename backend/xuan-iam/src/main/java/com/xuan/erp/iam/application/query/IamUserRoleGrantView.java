package com.xuan.erp.iam.application.query;

import java.util.List;

/**
 * IAM 用户角色授权视图，返回用户当前拥有的角色主键集合。
 */
public record IamUserRoleGrantView(
        Long tenantId,
        Long userId,
        List<Long> roleIds
) {
}
