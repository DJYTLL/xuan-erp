package com.xuan.erp.iam.application.command;

/**
 * IAM 登录命令，承载租户、用户名和明文密码。
 *
 * @param tenantId 当前登录租户 ID
 * @param username 登录用户名
 * @param password 明文密码，仅用于本次校验
 */
public record LoginIamUserCommand(Long tenantId, String username, String password) {
}
