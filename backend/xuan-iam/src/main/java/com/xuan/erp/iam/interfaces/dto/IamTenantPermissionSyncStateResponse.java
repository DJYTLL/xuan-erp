package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 侧租户权限同步状态响应。
 */
public record IamTenantPermissionSyncStateResponse(
        @Schema(description = "租户 ID")
        Long tenantId,
        @Schema(description = "IAM 最后一次成功同步的套餐权限指纹")
        String lastSyncedPermissionHash
) {
}
