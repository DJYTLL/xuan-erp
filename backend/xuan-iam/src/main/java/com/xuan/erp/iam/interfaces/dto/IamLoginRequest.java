package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 登录请求体。
 *
 * @param tenantId 兼容旧登录契约的当前登录租户 ID
 * @param tenantCode 当前登录租户编码
 * @param username 登录用户名
 * @param password 登录密码
 */
@Schema(description = "IAM 登录请求体")
public record IamLoginRequest(
        @Schema(description = "兼容旧登录契约的当前登录租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "当前登录租户编码", example = "acme")
        String tenantCode,
        @Schema(description = "登录用户名", example = "admin")
        String username,
        @Schema(description = "登录密码", example = "Passw0rd!")
        String password
) {
}
