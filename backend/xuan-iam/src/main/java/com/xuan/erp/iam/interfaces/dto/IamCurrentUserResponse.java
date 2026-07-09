package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * 当前用户响应体，返回 JWT 解析出的轻量身份信息。
 *
 * @param userId 当前登录用户 ID
 * @param tenantId 当前访问租户 ID
 * @param username 当前登录用户名
 * @param roles 角色编码集合
 * @param authVersion 授权快照版本
 * @param permissions 权限编码集合
 */
@Schema(description = "当前用户响应体")
public record IamCurrentUserResponse(
        @Schema(description = "当前登录用户 ID", example = "1")
        Long userId,
        @Schema(description = "当前访问租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "当前登录用户名", example = "admin")
        String username,
        @Schema(description = "角色编码集合")
        Set<String> roles,
        @Schema(description = "授权快照版本", example = "7")
        Long authVersion,
        @Schema(description = "权限编码集合")
        Set<String> permissions
) {
}
