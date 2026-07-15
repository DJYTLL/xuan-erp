package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 权限命令，保持权限编码不可变，允许调整名称、归属和启用状态。
 */
public record UpdateIamPermissionCommand(
        String name,
        String serviceName,
        String menuCode,
        String description,
        Boolean enabled
) {
}
