package com.xuan.erp.iam.application.query;

import com.xuan.erp.common.security.CurrentUser;
import java.time.OffsetDateTime;

/**
 * IAM 登录结果视图，返回 access token、refresh token 和当前用户轻量信息。
 *
 * @param accessToken 已签发的访问令牌
 * @param accessTokenExpiresAt 访问令牌过期时间
 * @param refreshToken 已签发的刷新令牌明文，仅返回给客户端一次
 * @param refreshTokenExpiresAt 刷新令牌过期时间
 * @param currentUser 当前登录用户轻量身份信息
 */
public record IamLoginView(
        String accessToken,
        OffsetDateTime accessTokenExpiresAt,
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt,
        CurrentUser currentUser
) {
}
