package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 角色命令，保持租户和角色编码不可变。
 */
public record UpdateIamRoleCommand(
        String name,
        String description,
        Boolean enabled
) {
}
