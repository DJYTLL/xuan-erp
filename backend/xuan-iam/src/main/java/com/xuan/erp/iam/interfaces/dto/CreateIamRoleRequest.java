package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建 IAM 角色请求体，承载租户内角色的基础资料。
 */
@Schema(description = "创建 IAM 角色请求体")
public record CreateIamRoleRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "角色编码", example = "tenant_operator")
        String code,
        @Schema(description = "角色名称", example = "租户操作员")
        String name,
        @Schema(description = "角色说明", example = "负责日常业务操作")
        String description
) {
}
