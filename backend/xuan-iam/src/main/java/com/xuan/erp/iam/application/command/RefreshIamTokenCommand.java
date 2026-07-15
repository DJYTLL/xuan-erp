package com.xuan.erp.iam.application.command;

/**
 * IAM 刷新访问令牌命令，承载客户端提交的 refresh token 明文。
 *
 * @param refreshToken 刷新令牌明文，仅用于本次校验和轮换
 */
public record RefreshIamTokenCommand(String refreshToken) {
}
