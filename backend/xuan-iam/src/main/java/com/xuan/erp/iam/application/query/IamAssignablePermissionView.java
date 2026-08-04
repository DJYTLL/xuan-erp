package com.xuan.erp.iam.application.query;

/**
 * IAM 角色授权可选权限视图，只暴露授权展示需要的权限元数据。
 */
public record IamAssignablePermissionView(
        Long id,
        String code,
        String name,
        String serviceName,
        String menuCode,
        String description,
        boolean enabled
) {
}
