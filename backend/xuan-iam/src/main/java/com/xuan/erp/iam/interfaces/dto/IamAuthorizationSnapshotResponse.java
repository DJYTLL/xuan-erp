package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * IAM 授权快照响应体，返回用户当前角色、权限、菜单和列权限。
 */
@Schema(description = "IAM 授权快照响应体")
public record IamAuthorizationSnapshotResponse(
        @Schema(description = "授权快照 ID", example = "1")
        Long id,
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
        List<String> menuCodes,
        @Schema(description = "列权限快照")
        Map<String, Map<String, String>> columnSettings,
        @Schema(description = "快照哈希")
        String snapshotHash,
        @Schema(description = "构建时间")
        OffsetDateTime builtAt
) {
}
