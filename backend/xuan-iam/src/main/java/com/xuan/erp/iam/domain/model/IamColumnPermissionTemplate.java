package com.xuan.erp.iam.domain.model;

/**
 * IAM 列权限模板领域模型，角色绑定模板后获得模板中的字段访问规则。
 */
public record IamColumnPermissionTemplate(
        Long id,
        Long tenantId,
        String code,
        String name,
        String description,
        boolean enabled
) {
}
