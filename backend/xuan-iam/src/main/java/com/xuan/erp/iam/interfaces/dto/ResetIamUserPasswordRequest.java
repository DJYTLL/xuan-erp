package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 重置 IAM 用户密码请求体，密码由后端统一哈希。
 */
@Schema(description = "重置 IAM 用户密码请求体")
public record ResetIamUserPasswordRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "新密码", example = "NewPassw0rd!")
        String newPassword,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
