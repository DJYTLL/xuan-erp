package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 保存角色列权限模板绑定请求体。
 */
@Schema(description = "保存角色列权限模板绑定请求体")
public record SetIamRoleColumnPermissionTemplateRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "列权限模板 ID", example = "8")
        Long templateId,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
