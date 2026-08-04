package com.xuan.erp.iam.application.command;

/**
 * 创建 IAM 用户的应用命令，承载接口层传入的租户、账号和基础资料。
 */
public record CreateIamUserCommand(
        Long tenantId,
        String username,
        String initialPassword,
        String displayName,
        String email,
        String phone,
        String remark,
        String operator
) {
}
