package com.xuan.erp.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * IAM 用户授权快照领域模型，保存角色、权限、菜单和列权限的计算结果。
 */
public record IamAuthorizationSnapshot(
        Long id,
        Long tenantId,
        Long userId,
        Long authVersion,
        List<Long> roleIds,
        List<String> permissionCodes,
        List<String> menuCodes,
        Map<String, List<String>> columnSettings,
        String snapshotHash,
        OffsetDateTime expiresAt,
        OffsetDateTime builtAt,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
