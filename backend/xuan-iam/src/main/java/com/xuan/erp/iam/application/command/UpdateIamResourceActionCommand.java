package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 资源动作命令。
 */
public record UpdateIamResourceActionCommand(
        String actionName,
        String permissionCode,
        String description,
        Integer sortNo,
        Boolean enabled,
        String metadataJson,
        String operator
) {
}
