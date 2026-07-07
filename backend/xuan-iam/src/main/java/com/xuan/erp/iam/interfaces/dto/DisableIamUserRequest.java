package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 停用 IAM 用户请求体，包含停用原因和操作人。
 */
@Schema(description = "停用 IAM 用户请求体")
public record DisableIamUserRequest(
        @Schema(description = "停用原因", example = "员工离职")
        String reason,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
