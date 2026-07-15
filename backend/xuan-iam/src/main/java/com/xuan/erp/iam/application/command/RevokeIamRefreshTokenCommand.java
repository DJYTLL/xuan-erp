package com.xuan.erp.iam.application.command;

/**
 * @param refreshToken refresh token 明文，仅用于本次撤销前的哈希匹配
 */
public record RevokeIamRefreshTokenCommand(String refreshToken) {
}
