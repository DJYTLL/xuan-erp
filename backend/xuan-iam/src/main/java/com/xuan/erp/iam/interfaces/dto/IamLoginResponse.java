package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * IAM 登录响应体，返回 Bearer access token、refresh token 和当前用户信息。
 *
 * @param tokenType 令牌类型
 * @param accessToken 访问令牌
 * @param accessTokenExpiresAt 访问令牌过期时间
 * @param refreshToken 刷新令牌
 * @param refreshTokenExpiresAt 刷新令牌过期时间
 * @param currentUser 当前用户信息
 */
@Schema(description = "IAM 登录响应体")
public record IamLoginResponse(
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "访问令牌")
        String accessToken,
        @Schema(description = "访问令牌过期时间", example = "2026-07-08T10:15:30+08:00")
        OffsetDateTime accessTokenExpiresAt,
        @Schema(description = "刷新令牌")
        String refreshToken,
        @Schema(description = "刷新令牌过期时间", example = "2026-08-07T10:15:30+08:00")
        OffsetDateTime refreshTokenExpiresAt,
        @Schema(description = "当前用户信息")
        IamCurrentUserResponse currentUser
) {
}
