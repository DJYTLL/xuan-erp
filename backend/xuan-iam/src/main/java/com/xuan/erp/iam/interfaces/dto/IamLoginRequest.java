package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 登录请求体。
 *
 * @param tenantId 当前登录租户 ID
 * @param username 登录用户名
 * @param password 登录密码
 */
@Schema(description = "IAM 登录请求体")
public record IamLoginRequest(
        @Schema(description = "当前登录租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "登录用户名", example = "admin")
        String username,
        @Schema(description = "登录密码", example = "Passw0rd!")
        String password
) {
}
