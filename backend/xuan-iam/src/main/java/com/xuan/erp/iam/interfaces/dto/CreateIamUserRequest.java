package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建 IAM 用户请求体，包含租户、账号和用户基础资料。
 */
@Schema(description = "创建 IAM 用户请求体")
public record CreateIamUserRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "用户名，租户内大小写敏感", example = "admin")
        String username,
        @Schema(description = "密码哈希", example = "{bcrypt}$2a...")
        String passwordHash,
        @Schema(description = "显示名", example = "系统管理员")
        String displayName,
        @Schema(description = "邮箱", example = "admin@example.com")
        String email,
        @Schema(description = "手机号", example = "13800000000")
        String phone,
        @Schema(description = "备注", example = "默认管理员账号")
        String remark
) {
}
