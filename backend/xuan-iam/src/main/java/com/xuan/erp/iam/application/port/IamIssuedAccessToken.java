package com.xuan.erp.iam.application.port;

import java.time.OffsetDateTime;

/**
 * IAM 已签发访问令牌值对象。
 *
 * @param accessToken JWT access token 字符串
 * @param expiresAt 过期时间
 */
public record IamIssuedAccessToken(String accessToken, OffsetDateTime expiresAt) {
}
