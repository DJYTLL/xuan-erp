package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色列权限模板绑定响应 DTO。
 */
@Schema(description = "角色列权限模板绑定响应 DTO")
public record IamRoleColumnPermissionTemplateBindingResponse(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "角色 ID", example = "5")
        Long roleId,
        @Schema(description = "模板 ID", example = "8")
        Long templateId,
        @Schema(description = "模板编码", example = "tenant_readonly_masked")
        String templateCode,
        @Schema(description = "模板名称", example = "租户只读脱敏模板")
        String templateName
) {
}
