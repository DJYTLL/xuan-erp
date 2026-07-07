package com.xuan.erp.iam.application.command;

/**
 * 停用 IAM 用户的应用命令，记录停用原因和操作人。
 */
public record DisableIamUserCommand(
        String reason,
        String operator
) {
}
