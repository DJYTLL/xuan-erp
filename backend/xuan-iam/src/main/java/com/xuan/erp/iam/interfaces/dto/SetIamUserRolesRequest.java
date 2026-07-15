package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 设置 IAM 用户角色请求体。
 */
@Schema(description = "设置 IAM 用户角色请求体")
public record SetIamUserRolesRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "角色 ID 列表")
        List<Long> roleIds,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
