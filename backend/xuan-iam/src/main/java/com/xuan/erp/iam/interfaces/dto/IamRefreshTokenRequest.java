package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM refresh token 请求体。
 *
 * @param refreshToken 刷新令牌
 */
@Schema(description = "IAM refresh token 请求体")
public record IamRefreshTokenRequest(
        @Schema(description = "刷新令牌")
        String refreshToken
) {
}
