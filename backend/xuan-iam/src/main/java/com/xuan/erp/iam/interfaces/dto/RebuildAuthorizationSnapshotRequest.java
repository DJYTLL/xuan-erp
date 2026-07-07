package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 重建授权快照请求体，包含用户的角色、权限和菜单授权结果。
 */
@Schema(description = "重建 IAM 授权快照请求体")
public record RebuildAuthorizationSnapshotRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "用户 ID", example = "42")
        Long userId,
        @Schema(description = "权限版本", example = "7")
        Long authVersion,
        @Schema(description = "角色 ID 列表")
        List<Long> roleIds,
        @Schema(description = "权限编码列表")
        List<String> permissionCodes,
        @Schema(description = "菜单编码列表")
        List<String> menuCodes
) {
}
