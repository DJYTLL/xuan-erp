package com.xuan.erp.iam.application.command;

/**
 * 创建 IAM 角色命令，表示租户内可授权角色的基础资料。
 */
public record CreateIamRoleCommand(
        Long tenantId,
        String code,
        String name,
        String description
) {
}
