package com.xuan.erp.iam.application.command;

/**
 * 重置 IAM 用户密码的应用命令，由后端负责哈希明文新密码。
 */
public record ResetIamUserPasswordCommand(
        Long tenantId,
        String newPassword,
        String operator
) {
}
