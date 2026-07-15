package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建 IAM 权限请求体，承载全局权限目录中的权限点定义。
 */
@Schema(description = "创建 IAM 权限请求体")
public record CreateIamPermissionRequest(
        @Schema(description = "权限编码", example = "iam:role:update")
        String code,
        @Schema(description = "权限名称", example = "角色修改")
        String name,
        @Schema(description = "来源服务", example = "xuan-iam")
        String serviceName,
        @Schema(description = "所属菜单编码", example = "iam-role-management")
        String menuCode,
        @Schema(description = "权限说明", example = "修改角色资料和授权")
        String description
) {
}
