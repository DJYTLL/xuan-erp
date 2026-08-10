package com.xuan.erp.iam.domain.model;

/**
 * IAM 资源动作定义，表示业务资源可参与状态动作授权的动作。
 */
public record IamResourceAction(
        Long id,
        Long tenantId,
        String resourceKey,
        String actionCode,
        String actionName,
        String permissionCode,
        String description,
        Integer sortNo,
        boolean enabled,
        String metadataJson
) {
}
