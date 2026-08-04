package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 用户基础资料的应用命令，不承载用户名和密码变更。
 */
public record UpdateIamUserCommand(
        Long tenantId,
        String displayName,
        String email,
        String phone,
        Boolean enabled,
        String remark,
        String operator
) {
}
