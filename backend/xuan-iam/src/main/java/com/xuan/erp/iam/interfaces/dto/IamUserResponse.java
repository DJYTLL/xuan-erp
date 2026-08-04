package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * IAM 用户响应体，返回用户账号和安全状态的基础信息。
 */
@Schema(description = "IAM 用户响应体")
public record IamUserResponse(
        @Schema(description = "用户 ID", example = "42")
        Long id,
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "用户名", example = "admin")
        String username,
        @Schema(description = "显示名", example = "系统管理员")
        String displayName,
        @Schema(description = "邮箱", example = "admin@example.com")
        String email,
        @Schema(description = "手机号", example = "13800000000")
        String phone,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled,
        @Schema(description = "账号是否未锁定", example = "true")
        Boolean accountNonLocked,
        @Schema(description = "权限版本", example = "1")
        Long authVersion,
        @Schema(description = "备注", example = "默认管理员账号")
        String remark,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
