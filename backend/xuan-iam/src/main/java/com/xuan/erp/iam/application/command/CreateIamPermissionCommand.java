package com.xuan.erp.iam.application.command;

/**
 * 创建 IAM 权限命令，表示服务权限目录中的一个权限点。
 */
public record CreateIamPermissionCommand(
        String code,
        String name,
        String serviceName,
        String menuCode,
        String description
) {
}
