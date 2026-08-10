package com.xuan.erp.iam.application.command;

/**
 * 创建 IAM 资源动作命令。
 */
public record CreateIamResourceActionCommand(
        Long tenantId,
        String resourceKey,
        String actionCode,
        String actionName,
        String permissionCode,
        String description,
        Integer sortNo,
        Boolean enabled,
        String metadataJson,
        String operator
) {
}
