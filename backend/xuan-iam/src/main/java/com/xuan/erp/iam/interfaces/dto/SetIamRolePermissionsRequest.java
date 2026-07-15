package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 保存 IAM 角色权限请求体，使用权限编码集合替换角色当前授权。
 */
@Schema(description = "保存 IAM 角色权限请求体")
public record SetIamRolePermissionsRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "权限编码集合", example = "[\"iam:view\",\"iam:update\"]")
        List<String> permissionCodes,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
