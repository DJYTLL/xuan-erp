package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改 IAM 角色请求体，承载角色名称、说明和启用状态调整。
 */
@Schema(description = "修改 IAM 角色请求体")
public record UpdateIamRoleRequest(
        @Schema(description = "角色名称", example = "租户操作员")
        String name,
        @Schema(description = "角色说明", example = "负责日常业务操作")
        String description,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled
) {
}
