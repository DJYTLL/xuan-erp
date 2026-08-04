package com.xuan.erp.iam.application.command;

/**
 * IAM 登录命令，承载租户业务编码、兼容租户 ID、用户名和明文密码。
 *
 * @param tenantId 兼容旧登录契约的当前登录租户 ID
 * @param tenantCode 当前登录租户编码，面向用户输入
 * @param username 登录用户名
 * @param password 明文密码，仅用于本次校验
 */
public record LoginIamUserCommand(Long tenantId, String tenantCode, String username, String password) {

    public LoginIamUserCommand(Long tenantId, String username, String password) {
        this(tenantId, null, username, password);
    }
}
