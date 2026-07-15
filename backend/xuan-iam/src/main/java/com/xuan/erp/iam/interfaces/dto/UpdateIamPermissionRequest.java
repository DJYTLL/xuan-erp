package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改 IAM 权限请求体，承载权限名称、归属服务、菜单关联和启用状态调整。
 */
@Schema(description = "修改 IAM 权限请求体")
public record UpdateIamPermissionRequest(
        @Schema(description = "权限名称", example = "角色修改")
        String name,
        @Schema(description = "来源服务", example = "xuan-iam")
        String serviceName,
        @Schema(description = "所属菜单编码", example = "iam-role-management")
        String menuCode,
        @Schema(description = "权限说明", example = "修改角色资料和授权")
        String description,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled
) {
}
